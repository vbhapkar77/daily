# Daily — Documentation

> A personal daily check-in app for habits, with built-in capture for things you don't want to forget.

This folder is the **engineering source of truth** for the Daily project. Every significant decision, requirement, and tradeoff lives here in markdown, version-controlled alongside the code.

## How this folder is organized

```
docs/
├── README.md                     ← you are here
├── prd.md                        ← Product Requirements Document (single source of truth on what we build)
├── architecture.md               ← system diagram, request flow, deployment topology
├── dev-environment.md            ← every tool you need + install commands + verification
├── api/                          ← OpenAPI contract (source of truth for HTTP API)
│   ├── README.md
│   └── openapi.yaml
├── schema.md                     ← (later) finalized DB schema + indexes + migration plan
├── runbook.md                    ← (later) ops playbook — deploy, restore, on-call
├── glossary.md                   ← (later) shared vocabulary
│
├── decisions/                    ← Architecture Decision Records (ADRs)
│   ├── README.md                 ← What ADRs are; index of all ADRs
│   ├── 0001-auth-email-password.md
│   ├── 0002-streak-freeze-silent-grace.md
│   ├── 0003-email-provider-resend.md
│   ├── 0004-tech-stack.md
│   ├── 0005-postgres-on-neon.md
│   ├── 0006-monorepo-structure.md
│   ├── 0007-testing-strategy.md
│   ├── 0008-docker.md
│   ├── 0009-defer-kubernetes.md
│   ├── 0010-adopt-sdd-practices.md
│   └── 0011-java-25-supersedes-java-21.md
│
├── discussions/                  ← discussion / meeting notes
│   └── 2026-06-07-prd-review.md
│
└── learnings/                    ← Vishal's study notes — interview prep + concept reference
    ├── README.md
    └── TEMPLATE.md
```

## Doc index

### Start here
- 📄 **[PRD](prd.md)** — what we're building, for whom, and why (v1.0, locked)
- 🏗 **[Architecture](architecture.md)** — system diagram, request flow, deployment (v1.0, locked)
- 🛠 **[Dev environment setup](dev-environment.md)** — every tool you need + install commands

### Decisions
- 📑 **[ADR index](decisions/README.md)** — what ADRs are, format, process
- [ADR-0001](decisions/0001-auth-email-password.md) — Use email + password authentication
- [ADR-0002](decisions/0002-streak-freeze-silent-grace.md) — Streak survives one missed day per 7-day window
- [ADR-0003](decisions/0003-email-provider-resend.md) — Use Resend for transactional email
- [ADR-0004](decisions/0004-tech-stack.md) — Tech stack: Java/Spring Boot + Next.js/TypeScript
- [ADR-0005](decisions/0005-postgres-on-neon.md) — Postgres on Neon (reuse existing project)
- [ADR-0006](decisions/0006-monorepo-structure.md) — Single monorepo with `/backend` + `/frontend`
- [ADR-0007](decisions/0007-testing-strategy.md) — Full test pyramid with Testcontainers + Playwright
- [ADR-0008](decisions/0008-docker.md) — Docker for dev parity, integration tests, production image
- [ADR-0009](decisions/0009-defer-kubernetes.md) — Defer Kubernetes; use Render's managed orchestration
- [ADR-0010](decisions/0010-adopt-sdd-practices.md) — Adopt SDD practices (AGENTS.md, contract-first OpenAPI, per-feature specs)
- [ADR-0011](decisions/0011-java-25-supersedes-java-21.md) — Use Java 25 (LTS) instead of Java 21

### Discussions
- 🗣 [PRD review — 2026-06-07](discussions/2026-06-07-prd-review.md)

### Learnings
- 📚 [Learnings index](learnings/README.md) — Vishal's study notes
- 📝 [Note template](learnings/TEMPLATE.md)

### Coming soon
- `schema.md` — finalized DB schema with indexes + migration plan
- Live Swagger UI in dev once backend is running (`http://localhost:8080/swagger-ui.html`) — serves the YAML in `api/`
- `runbook.md` — deploy/rollback/restore/incident playbook
- First learning notes — when we start writing code and hitting concepts

## Documentation principles

1. **Docs live with code.** Markdown in git. No Notion, no Google Docs, no wiki. When the code changes, the docs change in the same PR.
2. **Decisions are append-only.** When a decision changes, write a new ADR that *supersedes* the old one. Never edit history.
3. **The PRD is the contract.** Scope creep is the #1 risk in single-builder projects. The PRD's non-goals section is the firewall. Adding scope = bumping the PRD version + change-log entry.
4. **Discussions are first-class.** Conversations that don't merit a formal ADR still get written up. A six-month-old chat history is unrecoverable; a discussion note isn't.
5. **Write for the next engineer.** Even if there's only one of you today, future-you and the hypothetical contributor on day 500 should be able to ramp from these docs alone.
6. **Learnings are different from decisions.** ADRs explain *the project's* choices. Learnings document *Vishal's* growing understanding of underlying concepts.

## How decisions get made

1. Identify a question that has multiple reasonable answers (if there's an obvious choice, no ADR needed).
2. Draft an ADR in the `proposed` status.
3. Review with stakeholders (Vishal + Claude in this project).
4. Mark `accepted` once decided. The ADR is now immutable.
5. If later we realize it was wrong, write a *new* ADR that supersedes the old one. The old one stays in the history.

## Status of the project

- ✅ Phase 0 — Product Requirements (PRD locked at v1.0)
- ✅ Phase 1 — Architecture & Setup (ADRs 0001-0009 accepted; architecture.md locked)
- 🟡 Phase 2 — Foundation (install tools, scaffold repo, auth + DB + CI/CD) — **next**
- ⬜ Phase 3 — Feature iteration
- ⬜ Phase 4 — Production hygiene (Sentry, runbook)
- ⬜ Phase 5 — Launch
