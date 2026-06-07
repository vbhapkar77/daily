# AGENTS.md

> Read this first if you are an AI agent (Claude Code, Cursor, Copilot, Aider, etc.) about to work on this repo.
> Time budget for reading: ~2 minutes. After that you should be able to operate.

---

## What this is

**Daily** — a personal daily check-in app for habits, with built-in capture for things you don't want to forget. Single-builder project (Vishal Bhapkar). Educational + practical: real production app following a formal SDLC.

For the full picture: [`docs/README.md`](docs/README.md). For the why-of-each-decision: [`docs/decisions/`](docs/decisions/README.md).

## Stack at a glance

| Layer | Choice |
|---|---|
| Backend | Java 21 + Spring Boot 3 + Maven (`./mvnw`) at `backend/` |
| Frontend | Next.js 15 (App Router) + TypeScript + Tailwind + shadcn/ui + pnpm at `frontend/` |
| Database | Postgres on Neon (prod); Postgres in Docker via `infra/docker-compose.yml` (local) |
| Auth | DIY email + password with bcrypt + JWT in HttpOnly cookie. See [ADR-0001](docs/decisions/0001-auth-email-password.md). |
| Tests | JUnit 5 + Mockito + Testcontainers (backend); Vitest + RTL + MSW + Playwright (frontend) |
| API contract | OpenAPI 3.1 at `docs/api/openapi.yaml` — **source of truth**, the controllers conform to it (not the other way around). See [ADR-0010](docs/decisions/0010-adopt-sdd-practices.md). |
| Deploy | Vercel (frontend), Render (backend Docker image), Neon (DB) |
| CI | GitHub Actions, path-filtered per subproject |

## Read these before changing things

| If you're about to... | Read first |
|---|---|
| Add or change a user-facing feature | [`docs/prd.md`](docs/prd.md) — find the relevant US-XX or F-XX requirement; confirm scope |
| Change architecture or pick a major tool | [`docs/decisions/`](docs/decisions/README.md) — see what's already decided. Don't silently override. Propose a new ADR if needed. |
| Modify an HTTP API route or DTO | [`docs/api/openapi.yaml`](docs/api/openapi.yaml) **first**, then implement to match |
| Start a new feature (anything non-trivial) | Create `specs/NNN-feature-name/` per [`specs/README.md`](specs/README.md). Spec → plan → tasks → implement. |
| Touch streak / check-in logic | [ADR-0002](docs/decisions/0002-streak-freeze-silent-grace.md) has the formal silent-grace rule. Tests in `backend/src/test/java/.../StreakTest.java` |
| Touch auth | [ADR-0001](docs/decisions/0001-auth-email-password.md). All auth routes have rate limits; never log passwords; bcrypt cost 12 |
| Change tests / coverage / CI | [ADR-0007](docs/decisions/0007-testing-strategy.md). Coverage gate is 90% on `lib/`, 80% global |

## How to run things

```bash
# 1. Start local Postgres
docker compose -f infra/docker-compose.yml up -d postgres

# 2. Backend (port 8080)
cd backend && ./mvnw spring-boot:run
# Swagger UI: http://localhost:8080/swagger-ui.html
# Health: http://localhost:8080/actuator/health

# 3. Frontend (port 3000)
cd frontend && pnpm install && pnpm dev

# Tests
cd backend && ./mvnw verify              # unit + integration (uses Testcontainers, needs Docker)
cd frontend && pnpm test                 # unit + component
cd frontend && pnpm test:e2e             # Playwright (backend must be running)

# Lint / typecheck (run before opening a PR)
cd backend && ./mvnw spotless:check
cd frontend && pnpm lint && pnpm typecheck

# Regenerate TS API client from OpenAPI (after editing docs/api/openapi.yaml)
cd frontend && pnpm openapi:gen
```

## Conventions — do

- Match the formatting tools (Spotless for Java, Prettier for TS). PRs that touch unrelated formatting will be rejected.
- Reference requirement IDs in commit messages and PRs: e.g. `feat(auth): implement signup (US-01, F-AUTH-1)`.
- Add a test for every new code path. Coverage gate is CI-blocking.
- Use parameterized queries (JPA / `@Query`). Never concatenate user input into SQL.
- Use `@AuthenticationPrincipal` to get the current user. Never trust client-provided `user_id` for authorization.
- Filter all queries by `user_id` at the service layer. Tests should verify cross-user isolation.
- Bcrypt cost 12 for new passwords. Generic error messages ("Invalid email or password") — no user enumeration.
- All env vars documented in `docs/dev-environment.md`. Never commit secrets.

## Conventions — don't

- Don't edit an `accepted` ADR. Write a new one that supersedes if a decision changes.
- Don't add a dependency without an ADR or strong justification in the PR description.
- Don't bypass the OpenAPI spec — if you need a new endpoint, add it to `docs/api/openapi.yaml` first.
- Don't introduce `any` in TypeScript. Don't introduce raw types in Java. Strict mode in both.
- Don't merge a PR with red CI. If a check is flaky, fix it or quarantine it — don't ignore.
- Don't use `System.out.println` / `console.log` in committed code. Use the logger.
- Don't store anything in browser `localStorage` that the server doesn't already know. The DB is the source of truth.
- Don't commit anything to `.env` / `.env.local` / `application-local.yml` — they're gitignored for a reason.

## When you're uncertain

- If a decision has multiple reasonable answers and isn't covered by an ADR: **stop, surface the choice to Vishal**, don't pick silently.
- If a spec file (`specs/NNN-.../spec.md`) is ambiguous: **flag the ambiguity in the PR description**, propose a resolution, don't assume.
- If you find yourself wanting to refactor unrelated code while working on a feature: **don't**. Open a separate PR or note it as follow-up.

## Where things live

| Looking for... | Look at |
|---|---|
| The product spec (what we're building) | [`docs/prd.md`](docs/prd.md) |
| The system architecture | [`docs/architecture.md`](docs/architecture.md) |
| Why we chose X | [`docs/decisions/`](docs/decisions/README.md) |
| API contract | [`docs/api/openapi.yaml`](docs/api/openapi.yaml) |
| Per-feature specs | [`specs/`](specs/README.md) |
| Dev environment setup | [`docs/dev-environment.md`](docs/dev-environment.md) |
| Vishal's study notes (Java/Spring concepts) | [`docs/learnings/`](docs/learnings/README.md) |
| Past discussions / decisions in progress | [`docs/discussions/`](docs/discussions/) |
| Backend code | `backend/src/main/java/...` |
| Frontend code | `frontend/app/`, `frontend/components/`, `frontend/lib/` |
| Infra config (Docker, scripts) | `infra/` |
| CI workflows | `.github/workflows/` |

## What success looks like for an AI contribution

A good PR from an AI agent on this repo:
- References the spec/ADR/PRD section it implements
- Updates `docs/api/openapi.yaml` *before* changing controllers (when applicable)
- Adds tests at the right level of the pyramid
- Has a clear description explaining *why*, not just *what*
- Passes CI on the first run
- Doesn't touch files outside its scope

If you're a human reviewing this file: this represents the project's expectations of *any* contributor, AI or human.
