# Changelog

All notable changes to Daily are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Production hygiene:** Dependabot config (weekly Maven + npm + Actions updates), CodeQL SAST workflow, PR title Conventional Commits check, CODEOWNERS, issue templates (bug + feature), PR template, SECURITY.md vulnerability disclosure policy, CONTRIBUTING.md workflow guide, this CHANGELOG. README badges added.
- **Spec 001 (Auth) — T-001:** JJWT 0.12.6 dependencies (`jjwt-api` @ compile, `jjwt-impl` and `jjwt-jackson` @ runtime). Learning note on Maven scopes + api/impl split pattern.
- **Spec 001 — drafted & approved:** signup, login, logout, password reset feature spec (16 user scenarios, 16 ACs, 40 atomic tasks across 10 phases). Per OQ-1 resolution, rate limiter is DB-backed (table `rate_limit_events`).

### Changed

- ADR-0005 (Postgres on Neon) amended with project-renaming rationale explaining why we share the Neon project across multiple personal apps.
- ADR-0011 (Java version) clarified: **JDK 25 runtime, Java 21 source/target** for Spring Boot 3.5 compatibility.

---

## [0.2.0] — 2026-06-07 — Cloud-deployed empty stack

### Added

- **Backend deployed to Render** at https://daily-api-nteb.onrender.com (multi-stage Docker, eclipse-temurin:25-jre-alpine).
- **Frontend deployed to Vercel** at https://daily-seven-olive.vercel.app (Next.js 16 + Turbopack).
- **Cross-origin wiring:** CORS env-var driven (`CORS_ALLOWED_ORIGINS`); Vercel ↔ Render confirmed end-to-end.
- **OpenAPI contract** served at `/openapi.yaml` and rendered via Swagger UI at `/swagger-ui/index.html`.
- **Backend HealthBadge** component on the frontend: live probes the backend `/actuator/health` and shows status.

### Infrastructure

- Local Postgres via Docker Compose (`postgres:16-alpine`).
- 3 GitHub Actions workflows (backend, frontend, docs) with path filters.
- Multi-stage Dockerfile (~150 MB runtime image, non-root user, healthcheck).

---

## [0.1.0] — 2026-06-07 — Foundation

### Added

- **PRD v1.0** locked with 15 user stories, functional + non-functional requirements, success metrics.
- **11 ADRs:**
  - 0001: Email + password authentication (vs. OAuth)
  - 0002: Streak survives one missed day per 7-day window (silent grace)
  - 0003: Resend for transactional email
  - 0004: Tech stack — Java/Spring Boot + Next.js/TypeScript
  - 0005: Postgres on Neon (shared project, separate `daily` database)
  - 0006: Single monorepo with `/backend` + `/frontend` subdirs
  - 0007: Full test pyramid with Testcontainers + Playwright
  - 0008: Docker for dev parity + production image
  - 0009: Defer Kubernetes; use Render's managed orchestration
  - 0010: Adopt SDD practices (AGENTS.md, contract-first OpenAPI, per-feature specs)
  - 0011: JDK 25 LTS runtime (Java 21 source level for Spring Boot 3.5)
- **Architecture document** with system diagram, request lifecycle, test pyramid, CI/CD pipeline, production topology.
- **AGENTS.md** AI orientation file at repo root.
- **`specs/TEMPLATE/`** per-feature SDD workflow templates (spec.md / plan.md / tasks.md).
- **`docs/api/openapi.yaml`** skeleton with shared schemas + first endpoints.
- **`docs/learnings/`** notebook for study notes (interview prep).
- Initial Spring Boot 3.5.0 scaffold (with web, data-jpa, security, validation, actuator, flyway, postgresql, testcontainers, devtools, configuration-processor, springdoc).
- Initial Next.js 16 scaffold (TypeScript strict, Tailwind 4, ESLint, Turbopack).

### Known issues

- Spotless code formatting plugin disabled — upstream JDK 25 compat issue with google-java-format and palantir-java-format. See [`docs/discussions/2026-06-07-spotless-jdk25.md`](docs/discussions/2026-06-07-spotless-jdk25.md).
- Testcontainers fails locally on Docker Desktop 4.76.0 (HTTP 400 from `/info`). See [`docs/discussions/2026-06-08-testcontainers-docker-desktop.md`](docs/discussions/2026-06-08-testcontainers-docker-desktop.md). Works fine on CI's Linux Docker.

---

[Unreleased]: https://github.com/vbhapkar77/daily/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/vbhapkar77/daily/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/vbhapkar77/daily/releases/tag/v0.1.0
