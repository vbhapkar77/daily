# Contributing to Daily

Thanks for your interest. This is currently a single-builder project, but we operate it
as if a team of five could join tomorrow — which means clean docs, real workflows, and
predictable conventions.

## TL;DR — the contribution workflow

```
1. Read AGENTS.md and docs/README.md to orient
2. Read the spec for the feature you want to work on (specs/NNN-name/spec.md)
3. Pick a task from specs/NNN-name/tasks.md
4. Create a branch: feat/auth-NNN-short-description
5. Make the change + tests + (often) a learning note
6. Open a PR with the template filled in
7. CI must go green; reviewer must approve; then merge (squash)
```

For the full philosophy and structure, see [`docs/README.md`](docs/README.md).

---

## Setting up locally

See [`docs/dev-environment.md`](docs/dev-environment.md) — it walks through every tool
(JDK, Docker, IntelliJ, pnpm, etc.) with install commands and verification steps.

The shortest possible "get running":

```bash
# Postgres in Docker
docker compose -f infra/docker-compose.yml up -d postgres

# Backend (separate terminal)
cd backend && ./mvnw spring-boot:run

# Frontend (separate terminal)
cd frontend && pnpm install && pnpm dev
```

Backend on http://localhost:8080. Frontend on http://localhost:3000.
Swagger UI at http://localhost:8080/swagger-ui/index.html.

---

## Project conventions (the short list)

The full conventions live in [`AGENTS.md`](AGENTS.md). The non-negotiable ones:

- **Branch per task.** Never push to `main` directly. Branch protection enforces this.
- **One PR per task.** Tiny PRs are reviewable; giant PRs are not.
- **Conventional commits on PR titles.** `feat(scope): ...`, `fix(scope): ...`, etc. Enforced by `pr-title` workflow.
- **Tests are required.** Coverage gate is 90% on `lib/` business logic, 80% global. CI blocks below threshold.
- **No `any` in TypeScript, no raw types in Java, no commented-out code.** Strict everywhere.
- **No secrets in the diff. Ever.** All secrets are env vars on Vercel/Render.
- **Documentation lives with code.** When a non-obvious change goes in, add a learning note in `docs/learnings/`.

---

## Where to put things

| You want to... | Look at |
|---|---|
| Understand *what* we're building | [`docs/prd.md`](docs/prd.md) |
| Understand *why* a tech choice was made | [`docs/decisions/`](docs/decisions/) (ADRs) |
| Add a new feature | [`specs/`](specs/) — copy `TEMPLATE/`, write spec.md, plan.md, tasks.md |
| Add/change an HTTP endpoint | [`docs/api/openapi.yaml`](docs/api/openapi.yaml) FIRST, then implement |
| Document a concept you learned while coding | [`docs/learnings/`](docs/learnings/) |
| Report a security issue | [`SECURITY.md`](SECURITY.md) |
| Report a bug | [GitHub Issues](https://github.com/vbhapkar77/daily/issues/new/choose) |

---

## Making your first PR

Follow this sequence the first time and you'll be aligned with our workflow.

1. **Sync `main` and create a branch:**
   ```bash
   git checkout main && git pull
   git checkout -b feat/auth-NNN-your-task
   ```

2. **Make the change.** Run tests as you go.
   ```bash
   cd backend && ./mvnw -B verify    # backend
   cd frontend && pnpm test          # frontend (when test runner is set up)
   ```

3. **Commit using Conventional Commits format:**
   ```bash
   git commit -m "feat(auth): add signup endpoint (S001 T-015)"
   ```

4. **Push and open a PR:**
   ```bash
   git push -u origin feat/auth-NNN-your-task
   gh pr create
   ```
   The PR template will pre-fill; fill in the sections.

5. **Wait for CI.** Three workflows might run depending on what you changed (`backend`, `frontend`, `docs`). All must pass.

6. **Request review** (auto-requested via CODEOWNERS).

7. **Squash-merge** once approved. Delete the branch.

---

## Branch naming

| Prefix | When | Example |
|---|---|---|
| `feat/` | New feature or enhancement | `feat/auth-015-signup-endpoint` |
| `fix/` | Bug fix | `fix/cors-preflight-on-render` |
| `chore/` | Tooling, deps, build, infra | `chore/upgrade-spring-boot-3.6` |
| `docs/` | Documentation only | `docs/add-deployment-runbook` |
| `spec/` | New or updated feature spec | `spec/002-habits-crud-draft` |
| `refactor/` | Code change with no behavior change | `refactor/extract-user-mapper` |

---

## Commit messages

We use **Conventional Commits**. The format:

```
type(scope): subject

[optional body]

[optional footer]
```

Allowed `type` values: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`, `spec`.

Examples:

```
feat(auth): implement signup endpoint with bcrypt hashing (S001 T-015)
fix(security): close CORS leak on /api/v1/admin
docs(learnings): add note on Spring Security filter chain
chore(deps): bump spring-boot 3.5.0 → 3.5.1
spec(002): approve habits CRUD spec
```

Why? It makes the git log self-explanatory, lets us generate CHANGELOGs automatically,
and is the de-facto industry standard.

---

## Tests

- **Unit tests** for pure logic. Fast, isolated.
- **Integration tests** for anything touching the DB or HTTP layer. Use Testcontainers (real Postgres).
- **E2E tests** (Playwright) for critical user flows.

If you can't write a test for your change, ask whether the change is in the right place
or the right shape.

Run locally:

```bash
cd backend && ./mvnw -B verify    # all backend tests + coverage report
cd frontend && pnpm test          # unit + component
cd frontend && pnpm test:e2e      # e2e (Playwright; backend must be running)
```

---

## Code style

- **Java:** when Spotless is re-enabled (see [discussion](docs/discussions/2026-06-07-spotless-jdk25.md)), it enforces Palantir Java Format. Until then, IntelliJ's default formatter is fine.
- **TypeScript:** ESLint + Next.js's preset. Run `pnpm lint` and `pnpm typecheck` before committing.
- **Markdown:** no specific linter; just be readable.

---

## Adding dependencies

Adding a new library is a real decision. Before doing it:

1. Does the standard library / Spring / Next.js already cover this?
2. Is the library actively maintained (recent commits, releases)?
3. Does it have a sane license (MIT, Apache 2.0, BSD)?
4. Does it pull in transitive deps we're uncomfortable with?

If yes to (2-4) and no to (1), add it — but mention the reasoning in your PR description.

For major dependencies (new framework, new ORM, etc.), open an ADR first.

---

## Asking questions

- Stuck on setup? Re-read [`docs/dev-environment.md`](docs/dev-environment.md); ask in an issue if still stuck.
- Disagreement with a decision? Read the relevant ADR. If still disagree, open a new ADR proposing the change.
- Want to discuss something open-ended? Use [GitHub Discussions](https://github.com/vbhapkar77/daily/discussions) (if enabled).

---

Thanks again. Make something good.
