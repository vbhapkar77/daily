# Daily

> A personal daily check-in app for habits, with built-in capture for things you don't want to forget.

Status: **🟡 Phase 1 → Phase 2** (architecture locked; repo scaffold next)

---

## What this is

Daily is a small, opinionated web app for people who want to be consistent about a few important daily habits (exercise, study, etc.) and have nowhere good to capture passing "don't forget this" thoughts.

For the full product picture: [docs/prd.md](docs/prd.md).

For the technical architecture: [docs/architecture.md](docs/architecture.md).

For the rationale behind each major choice: [docs/decisions/](docs/decisions/README.md).

## Tech stack at a glance

| Layer | Choice | Why |
|---|---|---|
| Backend | Java 21 + Spring Boot 3 + Maven | Industry-standard production stack; aligned with owner's Java mastery goal |
| Frontend | Next.js 15 + TypeScript + Tailwind + shadcn/ui | Modern React-based frontend, hosted on Vercel |
| Database | Postgres on Neon | Real production-grade SQL, free forever at our scale |
| Auth | Email + password, JWT-in-cookie sessions | DIY using Spring Security |
| Tests | JUnit 5 + Mockito + Testcontainers (backend); Vitest + Playwright (frontend) | Full pyramid, real DB in integration tests |
| Hosting | Render (backend), Vercel (frontend) | Both free tiers; CI/CD via GitHub Actions |
| Containers | Docker (multi-stage build) + Compose (local) | Dev/prod parity, simple local stack |

For every choice and what we rejected: [docs/decisions/](docs/decisions/README.md).

## Repository layout

```
daily/
├── backend/        Spring Boot service (Maven). Not yet scaffolded.
├── frontend/       Next.js 15 app (pnpm). Not yet scaffolded.
├── docs/           All design + decision documentation. Currently the entire content of the repo.
├── infra/          Docker Compose, CI configs, ops scripts. Not yet created.
└── .github/        CI workflows. Not yet created.
```

For the full structure rationale: [ADR-0006](docs/decisions/0006-monorepo-structure.md).

## Get started (once the repo is scaffolded)

```bash
# Prerequisites: see docs/dev-environment.md
docker compose -f infra/docker-compose.yml up -d postgres
cd backend && ./mvnw spring-boot:run    # backend on http://localhost:8080
cd ../frontend && pnpm dev              # frontend on http://localhost:3000
```

Full setup walkthrough: [docs/dev-environment.md](docs/dev-environment.md).

## Project status

This project follows the formal SDLC:

- ✅ **Phase 0 — Discovery & PRD** (v1.0 locked)
- ✅ **Phase 1 — Architecture & Setup** (9 ADRs + architecture doc locked)
- 🟡 **Phase 2 — Foundation** ← we are here. Next: scaffold repo, install tools, set up auth + DB + CI.
- ⬜ Phase 3 — Feature iteration
- ⬜ Phase 4 — Production hygiene (observability, runbook)
- ⬜ Phase 5 — Launch & iterate

## Contributing

This is a personal project, single-builder. If somehow you've stumbled across this and want to contribute, open an issue first.

## License

MIT (to be added in the LICENSE file).
