# Development Environment Setup

> **Read this first if you've just cloned the repo.** It walks you through every tool you need, how to install it, and how to verify it works. Time budget: ~15 minutes of actual time, mostly waiting for downloads.

---

## Operating system

This guide is written for **macOS** (the only OS we develop on currently). Linux instructions are straightforward equivalents; Windows users should use WSL2.

## Tool checklist

| Tool | Required? | What for | Install |
|---|---|---|---|
| Git | ✅ required | Version control | comes with macOS or `brew install git` |
| Homebrew | ✅ required | Installing the rest on macOS | https://brew.sh — one curl-bash command |
| JDK 21 (Temurin) | ✅ required | Compile + run Java backend | `brew install --cask temurin@21` |
| Maven Wrapper | ✅ included | Builds the backend (bundled with the repo as `./mvnw`) | no install — comes with project |
| Node.js 20+ | ✅ required | Frontend dev server | `brew install node` (or use `nvm` for multi-version mgmt) |
| pnpm | ✅ required | Frontend package manager (faster than npm) | `npm install -g pnpm` after Node is in place |
| Docker Desktop | ✅ required | Local Postgres + Testcontainers + production image | https://docs.docker.com/desktop/install/mac-install/ — or `brew install --cask orbstack` (lighter alternative) |
| IntelliJ IDEA Community | ✅ required | Java backend IDE | `brew install --cask intellij-idea-ce` |
| VS Code or Cursor | ✅ required | Frontend / docs editor | `brew install --cask visual-studio-code` or download Cursor |
| `gh` CLI | ✅ required | GitHub operations | `brew install gh` (already installed on this Mac) |
| `vercel` CLI | optional | Frontend deploys from terminal | `npm install -g vercel` (already installed) |
| DBeaver Community | optional but recommended | Visual DB browser | `brew install --cask dbeaver-community` |
| `psql` | optional | Command-line Postgres client | `brew install libpq && brew link --force libpq` |
| `httpie` or Postman | optional | API testing | `brew install httpie` or download Postman |

## Detailed install steps

### 1. Java 21 (Temurin)

```bash
brew install --cask temurin@21
```

Verify:
```bash
java --version
# Expected:
# openjdk 21.x.x ...
# Eclipse Adoptium build ...
```

If `java` resolves to a different version, set `JAVA_HOME`:
```bash
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
source ~/.zshrc
```

### 2. Docker Desktop (or OrbStack)

OrbStack is lighter and faster on Apple Silicon — recommended:
```bash
brew install --cask orbstack
open -a OrbStack
```

Or stock Docker Desktop:
```bash
brew install --cask docker
open -a Docker
```

Wait for the whale icon (or OrbStack icon) to indicate "running". Verify:
```bash
docker --version
# Docker version 24.x.x ...

docker compose version
# Docker Compose version v2.x.x

docker run --rm hello-world
# (downloads a tiny test image and prints "Hello from Docker!")
```

### 3. Node.js + pnpm

```bash
brew install node
npm install -g pnpm
```

Verify:
```bash
node --version       # v20.x.x or v22+
pnpm --version       # v9.x.x or newer
```

If you already have Node via `nvm`, just `nvm use 20` (or higher) before working on `frontend/`.

### 4. IntelliJ IDEA Community Edition

```bash
brew install --cask intellij-idea-ce
```

After first launch:
1. Open the `daily/backend/` folder (NOT the monorepo root — open the `backend/` subdirectory directly so IntelliJ recognizes it as a Maven project).
2. IntelliJ will detect `pom.xml` and offer to import dependencies — accept.
3. Plugins to enable from Settings → Plugins → Marketplace:
   - **Spring Boot Tools** (bundled in some versions)
   - **Lombok** (only if we decide to use Lombok; defer)
   - **Docker** (lets you manage containers from IDE)
   - **Database Tools and SQL** (bundled in Ultimate; Community lacks this — use DBeaver instead)
4. Configure JDK: Settings → Build → Compiler → Java Compiler → set Project bytecode version to 21.

### 5. (Optional) DBeaver

```bash
brew install --cask dbeaver-community
```

Use to connect to:
- **Local Postgres:** host `localhost`, port `5432`, database `daily`, user `daily`, password `daily`
- **Neon production `daily` DB:** copy the connection string from Neon dashboard; DBeaver has a one-click Postgres URL importer

### 6. (Optional) `gh` and `vercel` CLIs

Already installed and authenticated on this Mac. For a fresh machine:
```bash
brew install gh
gh auth login
gh auth setup-git    # wires gh as git credential helper

npm install -g vercel
vercel login
```

## Verifying the full stack works

After all installs, run from the monorepo root:

```bash
# 1. Start Postgres in a container
docker compose -f infra/docker-compose.yml up -d postgres

# Verify it's healthy
docker compose -f infra/docker-compose.yml ps
# postgres should show "healthy"

# 2. Start the backend (from a terminal, alternative to IntelliJ Run button)
cd backend
./mvnw spring-boot:run
# Wait for: "Started DailyApplication in X seconds"
# Backend now on http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html

# 3. Start the frontend (separate terminal)
cd ../frontend
pnpm install        # first time only
pnpm dev
# Frontend on http://localhost:3000

# 4. Open http://localhost:3000 in your browser. Sign up. Add a habit.
```

If all four steps succeed, your dev environment is fully set up.

## Environment variables

The following env vars are required to run the app. None are committed to git.

### Backend (`backend/src/main/resources/application-local.yml` or env vars)

| Variable | Local default | Production |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/daily` | Neon connection string (set in Render) |
| `DATABASE_USERNAME` | `daily` | from Neon string |
| `DATABASE_PASSWORD` | `daily` | from Neon string |
| `JWT_SECRET` | `local-dev-only-do-not-use-in-prod-32-chars-min` | randomly generated 32+ char string in Render |
| `RESEND_API_KEY` | (leave empty in local — emails skipped) | from Resend dashboard |
| `APP_BASE_URL` | `http://localhost:3000` | `https://daily.vercel.app` (or custom domain) — used in password reset links |

### Frontend (`frontend/.env.local`)

| Variable | Local default | Production |
|---|---|---|
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080` | `https://daily-api.onrender.com` |

`.env.local` files are gitignored. Sample files (`.env.example`) committed to repo for reference.

## Common issues

### "JAVA_HOME not set" when running `mvnw`
See section 1 above — add `JAVA_HOME` to your shell rc file and re-source it.

### Docker says "Cannot connect to the Docker daemon"
Make sure Docker Desktop / OrbStack is running. Look for the whale / OrbStack icon in your menu bar.

### Port 5432 already in use
Another Postgres is already running on your Mac. Either stop it (`brew services stop postgresql`) or change the host port in `infra/docker-compose.yml` (e.g., map to `5433:5432`).

### IntelliJ doesn't recognize the project as Maven
You opened the monorepo root instead of `backend/`. Close, and re-open `backend/` directly.

### Backend can't connect to Postgres
1. Verify Postgres container is running: `docker compose ps`
2. Verify it's healthy (not just "running"): the healthcheck must pass before Spring Boot can connect
3. Check `application-local.yml` has the right credentials (default: `daily` / `daily`)

### Frontend can't reach backend (`fetch failed`)
1. Verify backend is up on :8080: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`
2. Check `NEXT_PUBLIC_API_URL` in `frontend/.env.local`
3. Check browser console for CORS errors — backend's `application-local.yml` must allow `http://localhost:3000`

## Suggested terminal aliases

Drop these in your `~/.zshrc` for less typing:

```bash
alias daily-db-up='docker compose -f ~/claude-code-projects/daily/infra/docker-compose.yml up -d postgres'
alias daily-db-down='docker compose -f ~/claude-code-projects/daily/infra/docker-compose.yml down'
alias daily-db-shell='docker exec -it daily-postgres-1 psql -U daily -d daily'
alias daily-be='cd ~/claude-code-projects/daily/backend && ./mvnw spring-boot:run'
alias daily-fe='cd ~/claude-code-projects/daily/frontend && pnpm dev'
```

## Where to learn more

- **IntelliJ basics:** [official getting-started](https://www.jetbrains.com/help/idea/getting-started.html) — worth 30 min if you've never used it
- **Maven basics:** [docs](https://maven.apache.org/guides/getting-started/) — you'll rarely edit `pom.xml` but it's good to know
- **Docker basics:** [getting started](https://docs.docker.com/get-started/) — first 4 chapters are enough
- **Concepts as we hit them:** `docs/learnings/` — written from this project's perspective
