# Architecture

| | |
|---|---|
| **Status** | 🟢 Locked v1.0 (consistent with ADRs 0004–0009) |
| **Owner** | Vishal Bhapkar |
| **Last updated** | 2026-06-07 |

This document is the **technical overview** of Daily. For *what* we build and *why*, see [PRD](prd.md). For individual decisions, see [ADRs](decisions/README.md). This doc ties everything together at the system level.

---

## 1. One-paragraph summary

Daily is a two-tier web application. The browser loads a static Next.js frontend from Vercel. The frontend calls a JSON HTTP API hosted on Render — a Spring Boot 3 application running in a Docker container. The backend persists data to a Postgres database hosted on Neon (serverless). Outbound transactional email goes through Resend. Authentication is email-and-password using JWT-in-cookie sessions. All deploys are GitOps-driven from a single monorepo on GitHub.

---

## 2. High-level system diagram

```
                              ┌──────────────────────┐
                              │     User's browser    │
                              │  (mobile or desktop)  │
                              └────────────┬──────────┘
                                           │ HTTPS
                                           │
                ┌──────────────────────────┼──────────────────────────┐
                │                          │                          │
                │   Static frontend        │     JSON HTTP API        │
                ▼                          │                          ▼
        ┌───────────────┐                  │             ┌─────────────────────┐
        │    Vercel     │                  │             │       Render        │
        │  Next.js 15   │                  │             │   Spring Boot 3     │
        │  TypeScript   │                  │             │   Docker container  │
        │  Tailwind     │                  │             │   Java 21 JRE       │
        └───────┬───────┘                  │             └──────────┬──────────┘
                │                          │                        │
                │ fetch() with             │                        │ JDBC
                │ credentials              │                        │ (TLS)
                └──────────────────────────┘                        │
                                                                    ▼
                                                       ┌─────────────────────────┐
                                                       │       Neon              │
                                                       │   Postgres 16           │
                                                       │   database = `daily`    │
                                                       └─────────────────────────┘

                                                       ┌─────────────────────────┐
                              (outbound from backend)  │      Resend             │
                          ────────────────────────────►│   transactional email   │
                                                       │   (password reset, etc.)│
                                                       └─────────────────────────┘
```

---

## 3. Request lifecycle (the happy path)

A user marks a habit done. Trace through the system:

1. **Browser** — user taps the checkbox. React component dispatches a TanStack Query mutation: `POST /api/v1/checkins`.
2. **Vercel edge** — Next.js serves the cached static page; the API call goes directly to the backend (Vercel does *not* proxy). Browser knows the backend URL via build-time env (`NEXT_PUBLIC_API_URL`).
3. **CORS preflight** — first cross-origin request triggers an `OPTIONS` preflight. Spring Security's CORS filter responds with the allowed origin (`https://daily.vercel.app` or custom domain).
4. **Render → Backend container** — TLS terminates at Render's edge. Render proxies the plain HTTP request to the container on port 8080.
5. **Spring Security filter chain** — extracts the JWT from the `Authorization` cookie, validates signature + expiry, loads the `User` into the `SecurityContext`.
6. **Controller** — `@PostMapping("/api/v1/checkins")` deserializes the JSON body into a DTO, runs Bean Validation (`@Valid`), and delegates to the service.
7. **Service** — applies business logic: verifies the habit belongs to the user, computes new streak, opens a transaction.
8. **Repository** — Spring Data JPA `CheckinRepository.save(...)` issues an `INSERT ... ON CONFLICT DO UPDATE` (idempotent — re-tapping the same day doesn't duplicate).
9. **Hibernate** — emits SQL over the HikariCP connection pool to Neon Postgres.
10. **Postgres** — writes the row; transaction commits.
11. **Service** — returns the updated `CheckinDto` (now including the new streak number).
12. **Controller** — serializes to JSON, returns `201 Created`.
13. **Browser** — TanStack Query updates the cache; React re-renders the home screen with the new streak.

End-to-end latency target: < 400 ms p95 for warm requests.

---

## 4. Data flow & persistence

### Single source of truth

The Postgres `daily` database is the **only** authoritative store. There is no client-side persistent state (no `localStorage` of todos), no session cache outside JWT cookies, no in-memory cache that survives restart.

### Schema (preview)

A formal schema doc (with indexes and FK strategy) will be `docs/schema.md`. Preview:

```sql
users (
  id            BIGSERIAL PRIMARY KEY,
  email         CITEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  name          TEXT,
  timezone      TEXT NOT NULL DEFAULT 'UTC',
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

habits (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name        VARCHAR(50) NOT NULL,
  emoji       VARCHAR(8),
  sort_order  INT NOT NULL,
  archived_at TIMESTAMPTZ,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

checkins (
  id         BIGSERIAL PRIMARY KEY,
  habit_id   BIGINT NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
  date       DATE NOT NULL,                -- in user's local timezone
  done       BOOLEAN NOT NULL,
  note       TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (habit_id, date)
)

captures (
  id         BIGSERIAL PRIMARY KEY,
  user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  text       TEXT NOT NULL,
  remind_at  DATE,
  status     VARCHAR(16) NOT NULL DEFAULT 'open', -- open | done | dismissed
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

password_reset_tokens (
  id         BIGSERIAL PRIMARY KEY,
  user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash TEXT UNIQUE NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  used_at    TIMESTAMPTZ
)
```

### Migrations

Versioned SQL via **Flyway**, located at `backend/src/main/resources/db/migration/`. Naming convention: `V1__init.sql`, `V2__add_captures_index.sql`, etc. Flyway runs migrations on application boot, idempotently.

---

## 5. Authentication & session flow

Per [ADR-0001](decisions/0001-auth-email-password.md):

### Signup
```
Browser → POST /api/v1/auth/signup { email, password, name? }
Backend: validates → bcrypt(password, 12) → INSERT INTO users
       → issues JWT (uid, exp=30d, signed with HS256 secret)
       → Set-Cookie: session=<jwt>; HttpOnly; Secure; SameSite=Lax; Max-Age=30d
Browser: future requests automatically include the cookie
```

### Login
Similar: validates credentials against bcrypt hash, issues new JWT.

### Password reset
```
Browser → POST /api/v1/auth/forgot { email }
Backend: looks up user (or pretends to — no enumeration)
       → generates 32-byte random token
       → INSERT INTO password_reset_tokens (token_hash = sha256(token), expires_at = NOW() + 1h)
       → emails the plain token in a reset link via Resend
Browser: returns "If that email exists, we sent a link."

User clicks link → /reset?token=<plain>
Browser → POST /api/v1/auth/reset { token, new_password }
Backend: SELECT WHERE token_hash = sha256(token) AND expires_at > NOW() AND used_at IS NULL
       → if valid: UPDATE password, MARK token used, return success
       → if invalid: 400 (generic message)
```

### Session validation per request
Spring Security `OncePerRequestFilter` extracts JWT from cookie, verifies signature & expiry, populates `SecurityContext` with the user. Controllers see the user via `@AuthenticationPrincipal`.

### Logout
```
Browser → POST /api/v1/auth/logout
Backend: Set-Cookie: session=; Max-Age=0 (immediate expiry)
```

---

## 6. Local development environment

```
┌──────────────────────────────────────────────────────────────────┐
│  Vishal's Mac mini                                               │
│                                                                  │
│   ┌─────────────┐    ┌─────────────────┐    ┌─────────────────┐ │
│   │  IntelliJ   │    │  Cursor/VSCode  │    │  Docker Desktop │ │
│   │  backend/   │    │  frontend/      │    │                 │ │
│   │  port 8080  │    │  port 3000      │    │  ┌───────────┐  │ │
│   └──────┬──────┘    └────────┬────────┘    │  │ postgres  │  │ │
│          │ JDBC               │ HTTP        │  │ port 5432 │  │ │
│          │                    │             │  └───────────┘  │ │
│          └────────────────────┼────────►    │                 │ │
│                               │             │  ┌───────────┐  │ │
│                               │             │  │ (option)  │  │ │
│                               │             │  │  backend  │  │ │
│                               │             │  │  in       │  │ │
│                               │             │  │  Compose  │  │ │
│                               │             │  └───────────┘  │ │
│                               │             └─────────────────┘ │
└───────────────────────────────┼──────────────────────────────────┘
                                │
                                ▼
                       (frontend talks to local backend at localhost:8080)
```

The default dev flow:
1. `docker compose -f infra/docker-compose.yml up -d postgres` — starts Postgres
2. Open `backend/` in IntelliJ → Run main class — Spring Boot starts on :8080 with hot reload via DevTools
3. `cd frontend && pnpm dev` — Next.js on :3000, proxies API calls to :8080

The optional full-Docker flow (`docker compose -f infra/docker-compose.dev.yml up`) brings up backend + postgres in containers; used when working on infra changes or verifying production image behavior.

---

## 7. Test pyramid

```
                        ▲
                        │
                        │           ┌────────────────────┐
                        │           │    E2E (Playwright)│   ~15 tests, <30s each
                        │           │                    │   critical user flows only
                        │           └────────────────────┘
              SLOWER ─► │
                        │     ┌──────────────────────────────┐
                        │     │ Integration (SpringBootTest, │   ~50 tests, <5s each
                        │     │ Testcontainers + Postgres)   │   one per critical flow
                        │     └──────────────────────────────┘
                        │
                        │  ┌────────────────────────────────────────┐
                        │  │ Web layer / Repository tests           │   ~150 tests, <2s each
                        │  │ (@WebMvcTest, @DataJpaTest,            │   per controller/repo
                        │  │ Testcontainers)                        │
                        │  └────────────────────────────────────────┘
                        │
                        │ ┌─────────────────────────────────────────────────┐
                        │ │ Unit tests (JUnit 5, Mockito, Vitest)           │   ~500+ tests, <50ms each
                        │ │ Pure logic, services with mocked deps           │   bulk of test count
              FASTER ─► │ │ Frontend components, hooks, util fns            │
                        │ └─────────────────────────────────────────────────┘
                        │
                        ▼
```

Coverage gate: 90%+ on `lib/` (business logic). 80% global. See [ADR-0007](decisions/0007-testing-strategy.md).

---

## 8. CI/CD pipeline

```
Developer pushes a branch
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│                  GitHub Pull Request                    │
└─────────────────────────────────────────────────────────┘
         │
         │ Triggers (with paths: filter — only changed area runs)
         │
         ├──► backend.yml           ├──► frontend.yml
         │    - JDK 21 setup        │    - Node 20 setup
         │    - mvnw verify         │    - pnpm install
         │      • compile           │    - lint
         │      • unit tests        │    - typecheck
         │      • integration tests │    - unit tests
         │        (Testcontainers)  │    - component tests
         │      • JaCoCo coverage   │    - vitest coverage
         │      • Spotless lint     │    - Lighthouse CI
         │    - Build Docker image  │
         │      (verify it builds)  │
         │                          │
         └────────────┬─────────────┘
                      │
                      ▼
            ┌─────────────────────┐
            │  All checks green?  │
            └──────────┬──────────┘
                       │ yes
                       ▼
              Reviewer (or self) approves & merges
                       │
                       ▼
          ┌────────────────────────────┐
          │      Push to main          │
          └────────────┬───────────────┘
                       │
       ┌───────────────┼───────────────┐
       │               │               │
       ▼               ▼               ▼
┌────────────┐  ┌─────────────┐  ┌──────────────┐
│ Vercel     │  │  Render     │  │  e2e.yml     │
│ auto-deploy│  │  rebuilds   │  │  (Playwright │
│ frontend   │  │  Docker     │  │  against     │
│            │  │  image &    │  │  preview)    │
│            │  │  redeploys  │  │              │
└────────────┘  └─────────────┘  └──────────────┘
```

PR is blocked if any of: lint, typecheck, unit, integration, coverage threshold, or Lighthouse budget fail. See [ADR-0007](decisions/0007-testing-strategy.md) §CI gates.

---

## 9. Production deployment topology

```
                  ┌──────────────────────────────┐
                  │   github.com/vbhapkar77/daily│   single monorepo
                  └──────────────┬───────────────┘
                                 │
                                 │ on push to main
                                 │
              ┌──────────────────┼─────────────────────┐
              │                  │                     │
              ▼                  ▼                     ▼
        Vercel webhook      Render webhook       Cron-job.org
        (pulls /frontend)   (pulls /backend,     (pings /actuator/
                             builds Docker,       health every 14 min
                             redeploys)           to keep Render warm)
              │                  │
              │ <40 s            │ <3 min
              ▼                  ▼
   ┌───────────────────┐  ┌─────────────────────┐
   │  daily.vercel.app │  │ daily-api.onrender  │
   │  (static, global  │  │   .com (one         │
   │   CDN, no sleep)  │  │   container, free   │
   │                   │  │   tier, sleeps      │
   │                   │  │   without warm-     │
   │                   │  │   ping)             │
   └───────────────────┘  └──────────┬──────────┘
                                     │ TLS to *.aws.neon.tech
                                     ▼
                          ┌─────────────────────┐
                          │  Neon Postgres      │
                          │   database = daily  │
                          │   region = us-east-1│
                          └─────────────────────┘
```

---

## 10. Environments

| Environment | Frontend URL | Backend URL | Database |
|---|---|---|---|
| **local** | http://localhost:3000 | http://localhost:8080 | local Docker Postgres (port 5432) |
| **preview** (per PR) | `*.vercel.app` (Vercel preview URL) | same as production for now (no separate preview backend in v1) | same as production (acceptable for v1; document this risk) |
| **production** | `daily.vercel.app` (custom domain TBD) | `daily-api.onrender.com` | Neon `daily` DB |

**Caveat:** v1 uses the production DB for previews. This is a documented compromise for v1 — for personal-use scale it's fine. Future: a separate `daily_preview` database on Neon for preview environments.

---

## 11. Observability

| Concern | Tool | Notes |
|---|---|---|
| **Application logs** | Logback (backend) → stdout → Render's log tail | JSON format in production for greppability |
| **Error tracking** | Sentry free tier | Both backend and frontend instrumented |
| **Metrics** | Spring Boot Actuator + Micrometer | `/actuator/metrics` exposed; we don't push to a metrics backend in v1 |
| **Uptime monitoring** | cron-job.org (free) | Pings `/actuator/health` every 14 min |
| **Frontend perf** | Lighthouse CI (in PR pipeline) + Vercel Analytics (free tier) | TTI, CLS, LCP tracked over time |

Logging discipline:
- No PII in logs (no emails, no names — log user IDs only)
- Structured logs (key=value or JSON) — easy to filter
- Log levels:
  - `ERROR`: actionable failure (5xx response, exception that escaped)
  - `WARN`: non-critical but worth noting (rate limit hit, slow query)
  - `INFO`: high-level events (user signed up, deploy started)
  - `DEBUG`: development only, never in production

---

## 12. Security model

| Concern | Mitigation |
|---|---|
| **Passwords** | bcrypt with cost 12; plaintext never persisted or logged |
| **Session tokens** | JWT HS256 signed with secret from env var; HttpOnly+Secure+SameSite=Lax cookie |
| **CSRF** | SameSite=Lax cookie + no GET state mutation = CSRF-safe by default |
| **XSS** | React escapes by default; no `dangerouslySetInnerHTML`; CSP header set |
| **SQL injection** | All queries are parameterized via JPA/Hibernate; raw `String + var` SQL is forbidden |
| **Auth enumeration** | Login + forgot-password return the same generic message regardless of email existence |
| **Brute force** | Rate limit: 5 attempts / 15 min per (email + IP); lockout 15 min |
| **HTTPS** | Enforced by Vercel and Render at the edge; backend rejects plaintext HTTP |
| **Secrets in code** | None. All secrets in env vars (Render dashboard, Vercel project settings). Never logged. |
| **DB access** | Per-user data isolation enforced at every query (`WHERE user_id = :uid`). Single source-of-truth check at the service layer, double-checked in integration tests. |

---

## 13. Scale & performance budget

Personal-use scale assumptions:
- 1–10 active users (Vishal + a few friends he shares the link with)
- ~10 check-ins per user per day
- ~5 captures per user per day
- Total writes: <500/day. Reads: <5,000/day.

These numbers comfortably fit free tiers across every layer. We do not optimize for scale in v1 beyond:
- Indexes on FK columns and `(habit_id, date)` for the unique constraint
- Pagination on list endpoints (default 50, max 200)
- Standard HTTP caching headers on the frontend

The scale graveyard (things we explicitly are *not* building for in v1):
- Multi-region deployment
- Read replicas
- Caching layer (Redis)
- Sharding
- Message queues / async job processing
- Search engine (Elasticsearch / Meilisearch)

If any of these become needed, that's a great problem to have and gets its own ADR.

---

## 14. Where to find what

| If you need to... | Look at |
|---|---|
| Understand *what* the app does | [PRD](prd.md) |
| Understand a specific technical decision | [ADRs](decisions/README.md) |
| Set up your dev machine | [dev-environment.md](dev-environment.md) (coming next) |
| Read about a Java/Spring/SQL concept we're using | [learnings/](learnings/) (coming next) |
| Find a database column or relationship | `docs/schema.md` (coming when schema is finalized) |
| Find an API endpoint contract | `docs/api.md` (or live Swagger UI in dev) |
| Debug a production incident | `docs/runbook.md` (coming later) |
| Catch up on a past meeting / discussion | [discussions/](discussions/) |

---

## 15. Change log

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 2026-06-07 | Vishal + Claude | Initial version — synthesizes ADRs 0001–0009 into a single technical view |
