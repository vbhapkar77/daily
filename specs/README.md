# Per-feature specs

This folder is where we **specify each feature before we build it.** It's the operational arm of our spec-driven-development practice ([ADR-0010](../docs/decisions/0010-adopt-sdd-practices.md)).

---

## Why per-feature specs

A PRD captures *the product*. An ADR captures *a decision*. But neither captures the day-to-day work of **"we are about to build feature X — exactly what does it do, how does it fit, and what does done look like?"**

That's what these specs are for. They let us:
- **Reach alignment cheaply** — disagreements surface in a doc review, not in a code review after a week of work
- **Hand AI agents a clear brief** — better specs ⇒ better generated code
- **Trace tests back to intent** — every acceptance criterion becomes one or more tests
- **Avoid scope creep mid-implementation** — if it's not in the spec, it's not in this PR

The pattern is inspired by [GitHub Spec-Kit](https://github.com/github/spec-kit), but we use the *pattern*, not the *CLI tooling*. The valuable part is the four files per feature.

## The flow

For each feature:

```
   1. SPECIFY                2. PLAN                3. TASKS              4. IMPLEMENT
   ─────────                ───────                ───────              ────────────
   Write spec.md            Write plan.md          Write tasks.md       Work through
   - intent                 - approach             - small ordered      tasks in order
   - scenarios              - files changed         items                Each PR refs
   - acceptance criteria    - risks                - 1-3h each           the spec/task ID
   - out of scope           - test plan
   Reviewed & approved
   before any code
```

1. **Specify.** Write `spec.md`. Get alignment on *what and why* before *how*. This is the high-leverage step — questions surfaced here cost minutes; questions surfaced after implementation cost days.
2. **Plan.** Write `plan.md`. Translate the spec into a technical approach: which files, which APIs, which migrations. Identify risks.
3. **Tasks.** Write `tasks.md`. Break the plan into 1–3-hour atomic implementation units. Each task is independently testable.
4. **Implement.** Work through tasks in order. Each PR references the spec + task ID. Mark tasks done as they merge.
5. **Done.** When all acceptance criteria in `spec.md` are met and the spec status is updated to `done`, archive (or just leave) the folder.

## Folder structure per feature

```
specs/
├── README.md                    ← this file
├── TEMPLATE/                    ← copy this folder for each new feature
│   ├── spec.md
│   ├── plan.md
│   └── tasks.md
│
├── 001-auth-signup-and-login/   ← first real feature (when we get there)
│   ├── spec.md
│   ├── plan.md
│   ├── tasks.md
│   └── api.yaml                 ← OpenAPI fragment this feature adds
│
└── 002-...
```

## Numbering

Sequential, zero-padded to 3 digits. Once assigned, a number is never reused even if a spec is dropped. Use `001`, `002`, `003`...

## When to write a spec vs. just doing it

| Situation | Spec? |
|---|---|
| A new user-facing feature | Yes |
| A meaningful refactor that affects multiple files | Yes |
| Schema change | Yes |
| Adding a new external dependency | Probably (consider an ADR instead) |
| A small bug fix in one file | No — just fix it with a good PR description |
| Renaming a variable / formatting | No |
| Updating docs | No |

Rule of thumb: **if you'd want to discuss the approach with another engineer before coding, write a spec.**

## Quality bar

A good `spec.md`:
- Has user scenarios in Given/When/Then format (BDD-style — readable AND becomes test cases)
- Is unambiguous (no "we'll figure out the API later")
- Says what's *out of scope*, not just what's in scope
- Has acceptance criteria that are testable (not "feels fast" but "p95 < 400ms")
- References the PRD requirements it implements (US-XX, F-XX-X)

A bad `spec.md`:
- Is "we need a settings page" with no detail
- Conflates *what* and *how* (spec.md is what; plan.md is how)
- Sneaks in implementation decisions that should be in an ADR

## Status field

Each spec has a `status` at the top:

| Status | Meaning |
|---|---|
| `draft` | being written; not yet approved |
| `approved` | reviewed and locked; implementation can begin |
| `in-progress` | tasks being worked through |
| `done` | all acceptance criteria met, merged to main |
| `superseded by NNN` | replaced by a different spec |
| `dropped` | abandoned (keep the file with a note explaining why) |

## How specs interact with other docs

- **PRD** — defines product requirements at high level. Specs implement those requirements; each spec references the US-XX it satisfies.
- **ADRs** — define architecture decisions. Specs follow those decisions; if a spec needs to deviate, the spec's plan.md should call it out and probably propose a new ADR.
- **OpenAPI** (`docs/api/openapi.yaml`) — the canonical API spec. Each feature spec includes an `api.yaml` fragment that documents the endpoints it adds; these are merged into the canonical spec before implementation.
- **Learnings** (`docs/learnings/`) — Vishal's study notes. A feature spec might reference a learning ("see `learnings/spring/transactions.md` for why we use REQUIRED propagation here").

## Tip: keep specs LIGHT for trivial features

The point of SDD is to *reduce* total time, not increase it. A spec for "add a Cancel button to this modal" can be three sentences. A spec for "design the OAuth-callback handler" might be three pages. **Match the spec to the complexity of the feature.**

If you find yourself writing a spec that's longer than the implementation will be, ask: am I over-specifying?
