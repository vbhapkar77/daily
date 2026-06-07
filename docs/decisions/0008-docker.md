# ADR-0008 — Docker for local dev parity, integration tests, and production backend image

**Status:** Accepted
**Date:** 2026-06-07
**Decision makers:** Vishal Bhapkar
**Relates to:** [ADR-0004](0004-tech-stack.md), [ADR-0005](0005-postgres-on-neon.md), [ADR-0007](0007-testing-strategy.md), [ADR-0009](0009-defer-kubernetes.md)

---

## Context

Docker plays several roles in modern web app development. We need to decide which of them apply to Daily, because each choice affects the developer experience, CI complexity, and deployment topology.

The four candidate uses are:

1. **Local dev database** — run Postgres in Docker so devs don't install Postgres on their machine
2. **Local dev backend** — optionally containerize the backend in dev for testing how it behaves in a container
3. **Production backend image** — build a Docker image, deploy that to Render (instead of letting Render auto-build from Maven)
4. **Integration test containers** — Testcontainers spins up throwaway Postgres for each integration test run

Constraints and goals:
- Dev/prod parity is a non-negotiable principle. The container that runs in prod is the same one that runs locally and in CI.
- Cross-machine portability. If a contributor (or future-Vishal on a different laptop) clones the repo, `docker compose up` should bring up a working stack.
- Render expects either a buildpack-style auto-build OR a Dockerfile. Owning the Dockerfile gives us control.
- Vishal's learning goals: multi-stage builds, image size optimization, Compose for orchestration are all common interview topics.

## Decision

We use Docker in **all four** roles.

### 1. Local dev database (Postgres in Compose)

`infra/docker-compose.yml`:
```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: daily
      POSTGRES_PASSWORD: daily
      POSTGRES_DB: daily
    ports: ['5432:5432']
    volumes: ['daily-pgdata:/var/lib/postgresql/data']
    healthcheck:
      test: ['CMD-SHELL', 'pg_isready -U daily']
      interval: 5s
      retries: 5
volumes:
  daily-pgdata:
```

Run with: `docker compose -f infra/docker-compose.yml up -d postgres`

This gives every developer the same Postgres version as production (and the same as Neon's underlying engine).

### 2. Local dev backend (optional, opt-in)

A second Compose file `infra/docker-compose.dev.yml` includes the backend container. Used when:
- Working on the frontend without running Java
- Verifying the production Docker image behaves correctly
- Testing the full stack on a low-spec machine

Not used as the default dev loop (running the backend in IntelliJ is faster for iteration).

### 3. Production backend image

`backend/Dockerfile` — multi-stage build:

```dockerfile
# ---- build stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn ./.mvn
RUN ./mvnw -B dependency:go-offline
COPY src ./src
RUN ./mvnw -B package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
USER app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --quiet --spider http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
```

Key properties of this image:
- **Multi-stage build:** the heavy JDK + Maven only exist in the build layer. The final runtime image carries only the JRE (~150MB vs. ~600MB single-stage).
- **Alpine base:** smaller footprint than Debian/Ubuntu bases.
- **Non-root user (`app`):** standard hardening, prevents container escapes from being root on the host.
- **Healthcheck:** Docker reports container as unhealthy if Spring's `/actuator/health` doesn't respond.
- **Container-aware JVM flags:** `-XX:+UseContainerSupport` makes the JVM respect container memory limits (default in Java 11+ but explicit is clearer).
- **No Maven repository or build cache in the runtime image.**

Render is configured to build from this Dockerfile (set in Render's service config). On every push to `main`, Render pulls the repo, runs the multi-stage build, and runs the resulting image.

### 4. Testcontainers in integration tests

Already specified in [ADR-0007](0007-testing-strategy.md). Testcontainers (Java library) uses the local Docker daemon to spin up throwaway Postgres containers for each test run. Requires Docker Desktop running locally and on CI.

## Consequences

### Positive
- **True dev/prod parity.** The same Postgres image runs locally, in tests, and in production. The same backend Docker image runs in CI smoke tests and in production. Reduces "works on my machine" entire class of bug.
- **Zero install for collaborators.** A new contributor needs Docker Desktop and that's it for the DB. No "install Postgres 16, set up a database, run these CLI commands" instructions.
- **Trains real production muscle.** Multi-stage Dockerfile, Compose orchestration, healthchecks, non-root users — these are interview gold and common workplace skills.
- **Smaller deployment image.** ~150 MB final image vs. ~600 MB for a naive single-stage build. Render deploys faster.
- **Reproducible builds.** Same Dockerfile in 6 months produces the same image (modulo base image updates).
- **Easy local-only experiments.** Want to try Postgres 17 RC for a day? Change one line in Compose. No system-wide install.

### Negative
- **Docker Desktop is heavy on macOS.** It runs a Linux VM and consumes RAM (1–2GB resident). Acceptable on a modern Mac mini but noticeable.
- **First-time builds are slow.** Maven dependency download (`mvnw dependency:go-offline`) takes 1–2 minutes the first time. Subsequent builds are fast due to layer caching.
- **More moving parts in CI.** Testcontainers needs Docker-in-Docker (or socket-mount) on CI runners. GitHub Actions Linux runners have Docker available; macOS runners don't (we'll avoid macOS CI for backend).
- **License consideration for Docker Desktop.** Free for personal use and small businesses; large enterprises may need a paid license. Not our concern as personal project.

## Alternatives considered

### Alternative A — Native install of Postgres (no Docker)
**Rejected** because:
- Setup is fiddly: brew install postgres, create users, configure pg_hba.conf, etc.
- Each contributor's local Postgres may be a different version → bugs that don't reproduce.
- No isolation: messes with system-wide Postgres if you have one for another project.

### Alternative B — Embedded H2 / HSQLDB for local dev
**Rejected** because:
- Different DB engine than production → "works locally, breaks in prod" bugs (see [ADR-0007](0007-testing-strategy.md) for same reasoning re: tests).
- No JSONB, no Postgres-specific features.

### Alternative C — Render's buildpack auto-build (no Dockerfile)
**Rejected** because:
- Less control: Render picks Maven version, Java version, container OS, none of which we want to leave to opaque automation.
- Less learning: we miss the multi-stage build practice.
- Less portable: if we ever leave Render, we have to write the Dockerfile then anyway.

### Alternative D — Build the Docker image in CI and push to a registry; Render pulls the pre-built image
**Rejected for v1** because:
- Adds a registry (GitHub Container Registry or Docker Hub) as a dependency.
- Requires registry auth credentials in CI and Render.
- Marginal speedup: Render's build minutes are free and acceptable.
- Could revisit if Render build times become a bottleneck.

### Alternative E — Run the entire dev stack always in Docker (including the IDE-runnable backend)
**Rejected** because:
- Slower inner loop (rebuild image on every code change) vs. running Spring Boot in IntelliJ with DevTools (instant reload).
- Loses debugger integration (debugging through a container adds setup friction).
- The Compose backend stays as an *option* for full-stack smoke tests, not the default.

### Alternative F — Podman instead of Docker
**Rejected for v1** because:
- Functionally equivalent for our needs but more friction (Vishal is more familiar with Docker; ecosystem tooling assumes Docker).
- macOS support is less mature.

## Implementation notes

### Required developer setup
- Install Docker Desktop for Mac (or OrbStack as a lighter alternative — same Docker CLI compatibility)
- Verify with `docker --version` and `docker compose version`

### Compose conventions
- All compose files in `infra/`
- Volumes named with project prefix (`daily-pgdata`) to avoid colliding with other projects
- Named ports (5432) to avoid colliding with system Postgres if installed
- Use `:latest` only for one-off dev scratch; pin major versions everywhere else (`postgres:16-alpine` not `postgres`)

### Image size targets
- Backend production image: < 200 MB
- Track size in CI: `docker image inspect daily-backend --format='{{.Size}}'`

### Build cache hygiene
- `.dockerignore` excludes: `target/`, `.git/`, `*.md`, `.idea/`, anything not needed at runtime. Speeds builds and reduces context size.

### Secret handling in Compose
- Never hardcode secrets in `docker-compose.yml`. Use `.env` file (gitignored) for local secrets.
- Production secrets (`DATABASE_URL`, `RESEND_API_KEY`) come from Render env vars; the production Dockerfile doesn't bake any secrets in.

### Healthcheck endpoints
- Spring Boot Actuator's `/actuator/health` returns 200 OK when the app + DB are healthy. Used by Docker `HEALTHCHECK` and Render's health monitoring.

## References

- [Best practices for writing Dockerfiles](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)
- [Docker Compose specification](https://docs.docker.com/compose/compose-file/)
- [Testcontainers Postgres module](https://java.testcontainers.org/modules/databases/postgres/)
- [Spring Boot with Docker (official guide)](https://spring.io/guides/topicals/spring-boot-docker)
