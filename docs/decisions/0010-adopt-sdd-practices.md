# ADR-0010 — Adopt spec-driven development practices (AGENTS.md, contract-first OpenAPI, per-feature specs)

**Status:** Accepted
**Date:** 2026-06-07
**Decision makers:** Vishal Bhapkar
**Relates to:** [ADR-0006](0006-monorepo-structure.md) (repo structure), [ADR-0007](0007-testing-strategy.md) (tests trace to spec IDs)

---

## Context

In 2025–2026, AI-assisted development has shifted the bottleneck from "writing code" to "specifying intent precisely enough that AI produces correct code consistently." This has led to a renaissance of **spec-driven development (SDD)** practices that go beyond traditional documentation.

Three distinct industry currents motivate this shift:

1. **AI agents need high-fidelity context to operate well.** Tools like Claude Code, Cursor, Aider, and Copilot all benefit from structured project documentation that describes *intent*, not just *what code exists*. Repos with good `AGENTS.md` / `CLAUDE.md` files demonstrably get better AI output.

2. **Contract-first API design has matured.** OpenAPI tooling (Swagger Editor, Spectral linter, Prism mock server, OpenAPI Generator, SpringDoc) now makes "spec-then-implement" the smoother path, not the harder one. The traditional code-first approach causes spec drift; contract-first eliminates it by construction.

3. **Per-feature spec workflows** like GitHub's Spec-Kit have demonstrated that small, structured spec files (intent + plan + tasks) make features ship faster and with fewer regressions when AI is in the loop. The flow mirrors what well-run product teams already do (PRD → tech plan → tickets) but at higher fidelity.

Owner's stated context: Vishal is currently taking a Udemy course on spec-driven development. Adopting SDD on this project serves dual purposes — reinforcing the course learnings on a real codebase, and producing a portfolio artifact that demonstrates current industry practice.

We already practice "soft" SDD: a comprehensive PRD with stable IDs (US-01, F-AUTH-1), nine ADRs documenting major decisions with rejected alternatives, an architecture doc tying everything together. This puts us above the median modern startup. The remaining gap is **machine-readable specs** and **AI-native workflow scaffolding**.

## Decision

We will adopt three SDD practices, each addressing a different layer:

### 1. `AGENTS.md` at the repo root — AI orientation file

A single file at the repo root that orients any AI agent (Claude Code, Cursor, Copilot, etc.) in 30 seconds. Contents:
- One-line project summary
- Stack at a glance
- "Read first" pointers (PRD before features, ADRs before architecture changes)
- Commands to run (lint, test, build)
- Conventions (do / don't)
- Where to find specific things
- Escalation path ("if uncertain, ask Vishal")

The file is read automatically by every modern AI dev tool. Even if no human ever opens it, every AI interaction with this codebase becomes meaningfully better.

### 2. Contract-first API design via `docs/api/openapi.yaml`

The OpenAPI 3.1 spec at `docs/api/openapi.yaml` is the **single source of truth** for the HTTP API contract. The flow:

- **For new endpoints:** spec is updated in the YAML first (in the feature spec PR), then the Spring controller is implemented to match.
- **For the frontend:** TypeScript client types are generated from the spec on each build via `openapi-typescript`. No hand-maintained client.
- **For tests:** Spectral linter validates the spec for style/consistency. Schemathesis or Dredd can run contract tests against the running backend if we choose to add them later.
- **For documentation:** Swagger UI is served from the backend in dev (via SpringDoc reading our YAML, not generating it from controllers). The YAML is the authority; the UI is the view.

This inverts our originally planned flow (where SpringDoc generated the OpenAPI from controller annotations). The new flow is more disciplined: the contract is reviewed and approved before code is written.

### 3. Per-feature spec workflow at `specs/`

For each feature, we create a numbered folder following the Spec-Kit-inspired pattern:

```
specs/
├── README.md                    # workflow explanation
├── TEMPLATE/                    # copy this for each new feature
│   ├── spec.md
│   ├── plan.md
│   └── tasks.md
│
├── 001-auth-signup-and-login/   # first feature
│   ├── spec.md                  # WHAT and WHY (intent, scenarios, criteria)
│   ├── plan.md                  # HOW (approach, files, risks)
│   ├── tasks.md                 # ordered, small, atomic implementation tasks
│   └── api.yaml                 # OpenAPI fragment this feature adds
│
├── 002-habits-crud/
└── ...
```

The flow for each feature:

1. **Specify** (`spec.md`): write the intent, user scenarios (Given/When/Then BDD format), data model changes, API additions, acceptance criteria, and explicit out-of-scope items. Reviewed and approved before any code.
2. **Plan** (`plan.md`): break down the technical approach — files to be created/modified, risks, dependencies. AI proposes, human approves.
3. **Tasks** (`tasks.md`): ordered list of small (1–3 hour) implementation tasks. Each is independently testable and reviewable.
4. **Implement**: work through tasks in order. Each PR references the spec and task IDs.
5. **Done**: when all acceptance criteria pass and tests are green, mark the spec as completed.

This mirrors the workflow Vishal is learning in his Udemy course and matches the pattern that production AI-dev teams have converged on in 2025–2026.

## Consequences

### Positive
- **Better AI output across the board.** Every AI agent (Claude Code in our terminal, Cursor in our IDE, Copilot suggesting completions) gets meaningfully higher-fidelity context. Fewer "AI hallucinations" because the context constrains what's plausible.
- **No API drift between frontend and backend.** OpenAPI as source-of-truth means TypeScript client types are *always* in sync with what the backend serves. Eliminates a whole class of bugs.
- **Tests trace to acceptance criteria.** Each acceptance criterion in `spec.md` becomes a test (unit, integration, or e2e). Bidirectional traceability: "why does this test exist?" → "because of this acceptance criterion" → "because of this user story (US-XX in PRD)".
- **Spec changes are reviewable.** A diff to `spec.md` is more reviewable than a diff to a Java class — closer to product intent. Code review becomes "does the implementation match the spec?" rather than "is this code OK in isolation?"
- **Reinforces Vishal's Udemy learning.** Practicing SDD on this project compounds the course material with real-world application.
- **Strong interview narrative.** "Here's a project where we used spec-driven development end-to-end with AI assistance" is a current and differentiated story for senior backend interviews in 2026.

### Negative
- **More upfront work per feature.** Each new feature now starts with 30–60 minutes of spec writing before code. The payoff is fewer rewrites — but the upfront cost is real, especially for trivial features.
- **Risk of over-specification.** Spec purists waste time documenting obvious things. Mitigated by judgment — spec heavily for complex/uncertain features, lightly for obvious ones. We add a note to `specs/README.md` about this calibration.
- **OpenAPI YAML is verbose.** A 200-line YAML for a 50-line controller. Mitigated because the YAML pays back in generated client types + Swagger UI + mock server, all free.
- **Two files to keep in sync** (the per-feature `api.yaml` fragment and the consolidated `docs/api/openapi.yaml`). Mitigated with a build step that concatenates / validates them.
- **Slight learning curve.** Teams new to SDD often resist the "write spec first" discipline. For a single builder (Vishal), this is just self-discipline.

## Alternatives considered

### Alternative A — Stay with soft SDD (PRD + ADRs only, no AGENTS.md or per-feature specs)
**Rejected** because:
- We miss the AI-native productivity gains.
- We miss the opportunity to reinforce Vishal's course material.
- We accept silent spec drift as the project grows.

### Alternative B — Adopt only AGENTS.md (skip the rest)
**Rejected** because:
- AGENTS.md alone improves AI quality but doesn't address API drift or feature-level rigor.
- Cheap to add the other two at the same time; partial adoption sends mixed signals about practices.

### Alternative C — Adopt full GitHub Spec-Kit tooling (their CLI, conventions, templates)
**Rejected for v1** because:
- Spec-Kit's specific tooling adds yet another dependency to learn and maintain.
- We can adopt their *pattern* (numbered feature folders with spec.md/plan.md/tasks.md) without their *tooling*. The pattern is the valuable part.
- If we ever want their CLI's convenience, the migration is trivial (we'd already be using their file layout).

### Alternative D — Code-first with SpringDoc auto-generation (originally planned)
**Rejected** in favor of contract-first because:
- Code-first means OpenAPI drifts with every code change. Reviewers see Java diffs, not contract changes.
- Generated specs often miss niceties (proper examples, descriptions, security schemes) that hand-authored specs include naturally.
- The "fix" of careful annotation discipline is just as much work as writing the YAML directly.

### Alternative E — JSON Schema or Smithy instead of OpenAPI
**Rejected** because:
- OpenAPI has dominant ecosystem support (especially in Java/Spring world via SpringDoc).
- Smithy is excellent (AWS uses it internally) but lower adoption.
- JSON Schema is great for data models but doesn't cover the HTTP layer — OpenAPI does both.

### Alternative F — Full intent-driven / model-driven development (DSL → code generation)
**Rejected** because:
- Tooling is immature for our stack.
- Massive scope creep for a personal project.
- The marginal benefit over our chosen practices is small for our codebase size.

## What changes operationally

### Things we'll start doing
- Every new feature begins with a `specs/NNN-name/` folder. No exceptions for non-trivial features.
- API changes happen in `docs/api/openapi.yaml` first, then in the controller.
- AI agents are pointed at `AGENTS.md` first when contributing.

### Things we'll stop doing
- Free-form "let's just code this" sessions for features that have non-trivial scope.
- Hand-coding TypeScript types that mirror Java DTOs — codegen handles it.

### Things that stay the same
- PRD remains the contract for *what* we build at the product level.
- ADRs remain the contract for *how* we make significant technical choices.
- Discussion notes capture conversations that don't merit an ADR.
- Learnings folder captures Vishal's growing understanding of concepts.

## Implementation notes

- **Spectral linter config** at `docs/api/.spectral.yaml` — enforces consistent path naming, required descriptions, no inline schemas (use `$ref`).
- **Swagger UI in dev** — SpringDoc serves the static `openapi.yaml` at `/swagger-ui.html`. Spring is configured to NOT generate from controllers.
- **TS client codegen** in `frontend/package.json` script: `pnpm openapi:gen` runs `openapi-typescript ../docs/api/openapi.yaml --output ./lib/api-types.ts` before each build.
- **CI check:** Spectral lint runs on every PR. Backend's contract tests assert the running service matches the spec (using something lightweight like `schemathesis` against the local backend).

## References

- [GitHub Spec-Kit](https://github.com/github/spec-kit) — the most prominent open-source SDD workflow tooling (we adopt the pattern, not the CLI)
- [OpenAPI 3.1 spec](https://spec.openapis.org/oas/v3.1.0)
- [SpringDoc OpenAPI integration](https://springdoc.org/) — used for serving the spec, not generating it
- [Spectral](https://stoplight.io/open-source/spectral) — OpenAPI linter
- ["Spec-Driven Development with AI"](https://martinfowler.com/articles/) — broader writeup of the philosophy

## Change log

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 2026-06-07 | Vishal + Claude | Initial adoption |
