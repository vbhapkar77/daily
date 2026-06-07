# ADR-0006 — Single monorepo with `/backend` and `/frontend` subdirectories

**Status:** Accepted
**Date:** 2026-06-07
**Decision makers:** Vishal Bhapkar
**Relates to:** [ADR-0004](0004-tech-stack.md) (tech stack)

---

## Context

The app spans two distinct tech stacks (Java/Spring Boot backend and Next.js/TypeScript frontend). We need to decide the repository structure:

- **Monorepo:** one Git repo, multiple subprojects
- **Polyrepo:** separate Git repos per service (one for backend, one for frontend)
- **Hybrid:** monorepo for related code, separate repos for things like infra

This decision affects:
- How code review works (single PR vs. coordinated PRs)
- How CI is configured (one workflow file vs. multiple)
- How releases are tagged
- How easy it is for a contributor to clone and run everything
- How easy it is to share types/schemas between backend and frontend

We have prior experience from the demo projects (Pattern B used two separate repos: `vishals-todo-api` and `vishals-todo-classic`). That worked but created small annoyances: deploys had to be coordinated, the README in one repo had to link to the other, and the API URL was hard-coded across repo boundaries.

## Decision

**Single monorepo** at `~/claude-code-projects/daily/`, structured as:

```
daily/
├── backend/                  # Spring Boot application (Maven)
│   ├── pom.xml
│   ├── mvnw, mvnw.cmd
│   ├── src/main/java/...
│   ├── src/main/resources/...
│   ├── src/test/java/...
│   └── Dockerfile
│
├── frontend/                 # Next.js application (pnpm)
│   ├── package.json
│   ├── pnpm-lock.yaml
│   ├── next.config.ts
│   ├── tsconfig.json
│   ├── app/                  # App Router pages & layouts
│   ├── components/
│   ├── lib/
│   └── tests/
│
├── docs/                     # ALL documentation (this folder)
│   ├── prd.md
│   ├── architecture.md
│   ├── dev-environment.md
│   ├── decisions/            # ADRs
│   ├── discussions/          # meeting / review notes
│   └── learnings/            # study notes (interview prep)
│
├── infra/                    # Docker Compose, CI configs, ops scripts
│   ├── docker-compose.yml    # local dev: Postgres + (optionally) backend
│   ├── docker-compose.dev.yml
│   └── scripts/
│       └── bootstrap-neon.sql
│
├── .github/
│   └── workflows/            # CI: lint, test, deploy
│       ├── backend.yml
│       ├── frontend.yml
│       └── e2e.yml
│
├── .gitignore
├── README.md                 # top-level: project overview + getting started
├── CONTRIBUTING.md           # how to contribute (even if it's just you)
└── LICENSE
```

**Single Git repo** named `daily` published as `vbhapkar77/daily` on GitHub.

**CI strategy:** Each subproject has its own GitHub Actions workflow with `paths:` filter so only the changed area builds/tests. E.g., editing `frontend/**` doesn't trigger backend Java tests, and vice versa. The `e2e.yml` runs only on the `main` branch after both have deployed.

**Deployment:** Each subproject deploys independently from `main`:
- `frontend/**` changes → Vercel auto-deploys frontend
- `backend/**` changes → Render auto-deploys backend (or we manually trigger)

## Consequences

### Positive
- **Single source of truth.** Issues, PRs, releases, README all in one place. No "which repo do I file this in?" friction.
- **Coordinated full-stack changes in one PR.** A feature that touches both API contract and UI is one diff — reviewable end-to-end.
- **Shared documentation.** PRD, ADRs, architecture diagrams are project-wide, not stack-specific. Lives once at the top level.
- **Easier onboarding.** A contributor (or future-you) clones one repo and gets everything: code, docs, infra, CI. No multi-repo coordination.
- **Easier local dev.** `docker-compose up` from the top level brings up the whole stack. No "did I clone all three repos?" issues.
- **CI path filters keep it fast.** Backend changes don't run frontend tests, so the monorepo doesn't slow down CI.

### Negative
- **Mixed-tooling root.** The top level contains both `pom.xml` (under backend/) and `package.json` (under frontend/). Some tooling assumes a single language at the root; we have to be explicit about which subdir we're in.
- **Larger repo size.** Both `node_modules/` (ignored) and Maven cache add disk weight. Mitigated by good `.gitignore`.
- **CI configuration is more complex** than a single-language repo. Path filters and conditional jobs require care; easy to misconfigure and have a frontend bug slip into backend test results.
- **Permissions are coarse.** If we ever bring in collaborators, monorepo means everyone has access to everything. Acceptable for a personal project; could be a real concern at team scale.
- **Tighter coupling risk.** When code is in the same repo, the temptation to share too much (e.g., generated TypeScript types from Java classes that leak Java-isms) is real. We mitigate by treating the boundary at OpenAPI (backend publishes spec; frontend codegens client) — backend never directly imports anything from frontend or vice versa.

## Alternatives considered

### Alternative A — Polyrepo (two separate repos)
**Rejected** because:
- Already tried this with `vishals-todo-api` + `vishals-todo-classic`. Worked, but produced friction:
  - Two READMEs to keep in sync ("the API for this frontend lives at...")
  - Cross-repo issue tracking is awkward
  - Coordinated changes need two PRs that must merge together
  - Documentation (PRD, ADRs) has no obvious home in either
- Polyrepo makes sense when teams own different services (different ownership), different release cadences, or strict permission isolation. None apply here — single owner, joint releases, no permission concerns.

### Alternative B — Single-language monorepo (e.g., Java + JSP frontend, or Node + React in one project)
**Rejected** because:
- ADR-0004 already locked the two-stack decision. We're not collapsing to one language.

### Alternative C — Nx / Turborepo / Lerna monorepo tooling
**Rejected for v1** because:
- These tools shine for many JS subprojects with shared internal packages. We have exactly two subprojects with no shared internal packages (the boundary is HTTP / OpenAPI, not import).
- Adds tooling complexity (Nx config, project graph, task pipeline) for marginal benefit at our scale.
- Could revisit if we ever split frontend into multiple apps (web, marketing site, admin) — Turborepo would be the natural choice then.

### Alternative D — Backend and frontend in completely separate folders (not even sibling subfolders)
**Rejected** because: no benefit over the chosen sibling-subfolder layout, plus loses the "one clone, run everything" benefit.

## Implementation notes

- **Root `package.json`:** none. We don't need root-level npm scripts; each subproject has its own (`./backend/mvnw`, `cd frontend && pnpm dev`).
- **VS Code / Cursor workspace:** create a `.code-workspace` file that opens both `backend/` and `frontend/` as separate workspace folders for nicer IDE behavior. IntelliJ opens `backend/` only.
- **`.gitignore` at root:** covers both stacks (`node_modules/`, `target/`, `.idea/`, `.vscode/`, `.env*.local`, `.DS_Store`).
- **Top-level README:** must explain the structure, link to `docs/README.md`, and give the one-command "get started" recipe (something like: `docker compose up -d postgres; cd backend && ./mvnw spring-boot:run; cd ../frontend && pnpm dev`).
- **CI path filters example:**
  ```yaml
  on:
    pull_request:
      paths:
        - 'backend/**'
        - '.github/workflows/backend.yml'
  ```

## Future considerations

- **If we add a mobile app** (React Native), it becomes a third subfolder. Still a monorepo.
- **If we split into microservices** at some future scale, each service is a subfolder (e.g., `services/user/`, `services/habit/`). Still a monorepo until ownership truly splits.
- **If we want stricter boundaries between backend and frontend in CI**, we can add a `CODEOWNERS` file and protected paths.

## References

- [Monorepo vs. polyrepo trade-offs (DEV community summary)](https://dev.to/quickwit/monorepo-vs-polyrepo-1f6n) (informal but balanced)
- [GitHub Actions paths-filter syntax](https://docs.github.com/en/actions/using-workflows/triggering-a-workflow#using-filters)
