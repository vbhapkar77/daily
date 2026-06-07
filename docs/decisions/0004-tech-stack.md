# ADR-0004 — Tech stack: Java/Spring Boot backend, Next.js/TypeScript frontend

**Status:** Accepted
**Date:** 2026-06-07
**Decision makers:** Vishal Bhapkar
**Relates to:** [PRD §6](../prd.md#6-functional-requirements-mvp), ADRs [0005](0005-postgres-on-neon.md), [0007](0007-testing-strategy.md), [0008](0008-docker.md)

---

## Context

We need to choose the full technology stack before any code is written. The decision is shaped by three forces:

1. **Product needs (from PRD):** Full-stack web app with user accounts, per-user data, daily UI, server-side persistence, scheduled email reminders (v1.1). Mobile-first responsive. Fast (< 1.5s TTI on mid-range mobile). Multi-user, cross-device.

2. **Owner's learning goals:** Vishal has explicitly stated dual objectives:
   - Build something useful he'll dogfood
   - Master Java for career growth and interview readiness
   He needs to write meaningful production Java code, not just glue.

3. **Operational constraints:** Free-tier hosting throughout (Vercel + Render + Neon + Resend). Single-builder team (no platform engineers). Documentation-as-code, all decisions traceable.

The decision spans **two distinct stacks** (backend + frontend) so we evaluate them separately.

## Decision

### Backend
- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 3.x
- **Build:** Maven (with Maven Wrapper `./mvnw`)
- **Persistence:** Spring Data JPA + Hibernate ORM
- **Database driver:** PostgreSQL JDBC
- **Migrations:** Flyway (SQL-first)
- **Auth:** Spring Security 6 with JWT-in-cookie sessions
- **Validation:** Jakarta Bean Validation (`jakarta.validation.*`)
- **API docs:** SpringDoc OpenAPI 3 (auto-generated Swagger UI)
- **Testing:** JUnit 5 + Mockito + AssertJ + Spring Boot Test + Testcontainers
- **Hosting:** Render (free tier, with cron-job.org keep-warm)
- **Container:** Docker (multi-stage Dockerfile, JRE-only runtime image)

### Frontend
- **Language:** TypeScript (strict mode)
- **Framework:** Next.js 15 (App Router)
- **UI library / styling:** Tailwind CSS + shadcn/ui (copy-paste headless components)
- **Server state:** TanStack Query (React Query) v5
- **Form handling:** React Hook Form + Zod for schema validation
- **Testing:** Vitest + React Testing Library + MSW (for API mocking) + Playwright (e2e)
- **Hosting:** Vercel (free tier)

### Cross-cutting
- **CI:** GitHub Actions (free for public repos)
- **Error tracking:** Sentry (free tier)
- **Logging:** Logback (backend, JSON output for production)
- **Email:** Resend (already decided in [ADR-0003](0003-email-provider-resend.md))
- **Package manager (frontend):** `pnpm` (faster than npm, better disk usage)

## Consequences

### Positive
- **Career alignment:** Java + Spring Boot is the dominant enterprise backend stack in India (Razorpay, Flipkart, Swiggy, CRED, banking, fintech). Building this app gives Vishal concrete production Spring experience to discuss in interviews.
- **Modern Java features:** Java 21 brings records, sealed classes, pattern matching, virtual threads — these come up in senior interviews and reduce boilerplate significantly vs. Java 8 era code.
- **Mature ecosystem:** Spring Boot has been battle-tested for ~15 years. Documentation, Stack Overflow, library compatibility are all excellent. Few "you're holding it wrong" moments.
- **Industry-standard frontend:** Next.js is the highest-share React framework in 2026 production usage. Tailwind + shadcn is the same pairing used by Vercel, Linear, and many cutting-edge startups.
- **Real test pyramid possible:** Testcontainers + JUnit lets us test against real Postgres in CI — this is the *correct* way to test Spring apps and a frequent interview topic.
- **Auto-generated API docs:** SpringDoc reads the controllers and produces Swagger UI for free. Live API explorer = great DX + an artifact to show prospects.
- **Type safety on both ends:** TypeScript strict + Java's type system means most bugs are caught at compile time.

### Negative
- **Two runtimes, two ecosystems:** JVM + Node. Two package managers (Maven + pnpm). Two IDEs (IntelliJ for backend, Cursor/VS Code for frontend). More cognitive load than a Node-only stack.
- **Slower iteration than Node:** JVM cold start ~2–4s; Spring DevTools hot-reload helps but isn't as snappy as Next.js dev server. Acceptable cost for the learning benefit.
- **Heavier production footprint:** Spring Boot fat-jar ~50MB, JRE runtime image ~150MB. Compared to a tiny Node container (~50MB total). Doesn't matter at our scale.
- **More boilerplate than modern Node frameworks:** Entities + repositories + services + controllers + DTOs is more typing than a TypeScript+Prisma route handler. We accept this as a learning feature, not a bug.
- **Render's free tier sleeps the backend:** 15-minute idle → 20–40s wake. Mitigated by cron-job.org pinging `/health` every 14 minutes (free, see ADR-0009).
- **Frontend ↔ backend coordination:** Schema changes must be carried through both sides. Mitigated by an OpenAPI client codegen step (frontend types generated from backend's `/v3/api-docs.json`).

## Alternatives considered

### Backend alternatives

**Node.js + TypeScript + Express/Fastify**
- Rejected — same language as frontend means less context switching, faster iteration, smaller deployment, no JVM. But: doesn't serve Vishal's "master Java" goal. Would also lose the Spring ecosystem learnings (DI container, AOP, transactions, Spring Security) that are uniquely valuable interview material.

**Kotlin + Spring Boot**
- Rejected — modern, concise, runs on JVM. But: Vishal's stated goal is mastering *Java*, and most "Java jobs" in India ask Java-specific questions. Kotlin would be a worse fit for the explicit career goal. Could revisit for a future project.

**Java + Quarkus (or Micronaut)**
- Rejected — faster startup, smaller images, modern. But: significantly smaller talent pool / interview presence than Spring Boot. Spring Boot is what 90% of Java interviewers expect candidates to know.

**Python + Django / FastAPI**
- Rejected — clean, fast to ship. But: doesn't address Java goal. Different language, different ecosystem.

**Go + Gin/Echo**
- Rejected — fast, simple, beloved by infra teams. But: same reason as above; pivot away from Java mastery.

### Frontend alternatives

**Plain React + Vite (no Next.js)**
- Rejected — simpler, faster dev. But: we lose SSR (slower first paint), file-based routing (more setup), and the Next.js production conventions that match what large companies actually deploy. Worth the slight added complexity.

**Remix**
- Rejected — excellent framework, slightly nicer "web standards" mental model. But: smaller ecosystem, smaller talent pool, lower industry presence than Next.js. Next.js is the safer interview-relevant pick.

**SvelteKit**
- Rejected — smaller bundles, lovely DX. But: smaller ecosystem, less interview relevance, would require more from-scratch component work (no shadcn/ui equivalent).

**Vue / Nuxt**
- Rejected — strong in Asia, has growing market share. But: React ecosystem in India is dominant; React experience transfers more readily to common job openings.

### Build tool alternatives

**Gradle (instead of Maven)**
- Rejected for v1 — Gradle is faster and more powerful, but Maven is still the more common build tool in Java job postings, and Maven's declarative XML is friendlier for a first-time Java production project. Could revisit if we ever need Gradle's flexibility.

### State management (frontend) alternatives

**Redux Toolkit**
- Rejected — heavyweight for our needs; TanStack Query handles ~90% of the state we'd have (which is server state, not client UI state).

**Zustand**
- Rejected for v1 — great for client UI state, but our app has very little client-only state. We can add Zustand later for the ~5 things that need it.

### Auth library alternatives

**Auth0 / Clerk / Supabase Auth**
- Rejected — already rejected in [ADR-0001](0001-auth-email-password.md) for the broader auth strategy.

**Keycloak**
- Rejected — heavyweight, requires separate server. Overkill for a single-app project.

## Implementation notes

- **Java version:** Pin to Java 21 in `pom.xml`'s `<java.version>` property. Match in `Dockerfile` (use `eclipse-temurin:21-jre-alpine` or similar).
- **Spring Boot starters used:**
  - `spring-boot-starter-web` (REST endpoints)
  - `spring-boot-starter-data-jpa` (persistence)
  - `spring-boot-starter-security` (auth)
  - `spring-boot-starter-validation` (Bean Validation)
  - `spring-boot-starter-mail` (or Resend SDK directly)
  - `spring-boot-starter-actuator` (health, metrics endpoints)
- **Frontend client codegen:** generate TypeScript API client from backend's OpenAPI spec on each build. Tool: `openapi-typescript` or `@hey-api/openapi-ts`. Eliminates manual API contract sync.
- **Shared types:** none crossed via files; the OpenAPI codegen is the single source of truth.

## Tradeoff summary (one-liner)

We deliberately accept a more complex two-runtime stack and slower iteration than a Node-only project, in exchange for Vishal building real Spring Boot production experience and a portfolio piece that maps cleanly to common Indian backend engineering jobs.

## References

- [Spring Boot 3 reference guide](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Next.js 15 docs](https://nextjs.org/docs)
- [Vercel + Render + Neon architecture pattern](https://vercel.com/guides) (informal — common pattern but not a single canonical doc)
