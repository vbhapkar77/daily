# Architecture Decision Records (ADRs)

This folder is where we record **significant decisions** about the project — and just as importantly, **why** we made them and **what we rejected**.

## Why ADRs

Code shows *what* you decided. ADRs explain *why*. Six months from now, when you (or someone else) wonders "why didn't they just use OAuth?", the ADR has the answer — and the alternatives we considered, and the tradeoffs we accepted.

ADRs are immutable once accepted. If a decision changes, write a new ADR that **supersedes** the old one. Never edit history.

## When to write an ADR

Write one for:
- Tech stack choices (framework, ORM, hosting)
- Architectural patterns (auth strategy, state management approach)
- Tradeoffs with multiple reasonable options
- Decisions someone might second-guess later

Don't write one for:
- Variable naming
- Formatting / lint rules (those go in config)
- Trivial choices

If you can't think of an alternative anyone would consider, you probably don't need an ADR.

## Format

We use the lightweight **Michael Nygard** format:

1. **Title** — short, descriptive, prefixed with ADR number (e.g. `0001-auth-email-password.md`)
2. **Status** — proposed / accepted / deprecated / superseded by ADR-NNNN
3. **Context** — the situation when the decision was made; what forces were at play
4. **Decision** — what we decided, stated affirmatively
5. **Consequences** — both positive and negative outcomes of this decision
6. **Alternatives considered** — what else we looked at and why we rejected it

## Numbering

Sequential, zero-padded to 4 digits: `0001`, `0002`, …

## Index

| # | Title | Status | Date |
|---|---|---|---|
| [0001](0001-auth-email-password.md) | Use email + password authentication | Accepted | 2026-06-07 |
| [0002](0002-streak-freeze-silent-grace.md) | Streak survives one missed day per 7-day window | Accepted | 2026-06-07 |
| [0003](0003-email-provider-resend.md) | Use Resend for transactional email | Accepted | 2026-06-07 |
| [0004](0004-tech-stack.md) | Tech stack: Java/Spring Boot + Next.js/TypeScript | Accepted | 2026-06-07 |
| [0005](0005-postgres-on-neon.md) | Postgres on Neon (reuse existing project) | Accepted | 2026-06-07 |
| [0006](0006-monorepo-structure.md) | Single monorepo with `/backend` + `/frontend` | Accepted | 2026-06-07 |
| [0007](0007-testing-strategy.md) | Full test pyramid with Testcontainers + Playwright | Accepted | 2026-06-07 |
| [0008](0008-docker.md) | Docker for dev parity, integration tests, production image | Accepted | 2026-06-07 |
| [0009](0009-defer-kubernetes.md) | Defer Kubernetes; use Render's managed orchestration | Accepted | 2026-06-07 |
| [0010](0010-adopt-sdd-practices.md) | Adopt SDD practices (AGENTS.md, contract-first OpenAPI, per-feature specs) | Accepted | 2026-06-07 |
| [0011](0011-java-25-supersedes-java-21.md) | Use Java 25 (LTS) instead of Java 21 (supersedes part of ADR-0004) | Accepted | 2026-06-07 |
