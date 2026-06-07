# Spec: [Feature name]

| | |
|---|---|
| **Status** | `draft` |
| **Author** | Vishal |
| **Created** | YYYY-MM-DD |
| **PRD requirements** | US-XX, F-XXX-X, F-XXX-X |
| **Relates to ADRs** | ADR-NNNN, ADR-NNNN |
| **Related specs** | — |

---

## Goal

> One short paragraph: what does this feature accomplish for the user, and why does it matter? Write this before anything else. If you can't write this clearly, you don't understand the feature yet.

## User scenarios (Given / When / Then)

> BDD-style scenarios. Each scenario maps to one or more tests. Be specific about the actor (Anonymous user / Authenticated user / Admin / etc.) and the precondition.

### Scenario 1: [happy path name]

```
Given an [anonymous user] is on [page]
When they [action]
Then [observable outcome]
And [secondary outcome if applicable]
```

### Scenario 2: [edge case]

```
Given ...
When ...
Then ...
```

### Scenario 3: [error path]

```
Given ...
When they submit invalid input (e.g., empty email)
Then the request is rejected with [specific error]
And no [side effect] occurs
```

## Data model changes

> What changes in the database? New tables, new columns, indexes, constraints. Migration plan if non-trivial.

```sql
-- example
CREATE TABLE example (
  id BIGSERIAL PRIMARY KEY,
  ...
);

ALTER TABLE users ADD COLUMN ...
```

If no schema change: write "No data model changes."

## API additions

> List the HTTP endpoints this feature adds. Full contracts go in the accompanying `api.yaml` (OpenAPI fragment). Brief table here:

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/...` | Create ... |
| `GET` | `/api/v1/...` | Fetch ... |

If no API change: write "No API additions."

## UI changes

> Which pages or components are added/changed? Mockup or ASCII sketch if helpful. Link to actual designs if any.

## Out of scope (explicitly)

> What is NOT in this feature, even though it might seem related? This protects against scope creep.

- Item A — deferred to feature NNN
- Item B — explicitly never planned
- Item C — exists in PRD but not this feature

## Acceptance criteria

> The testable conditions for "done". Each becomes a test (or several). Use checkboxes so they're easy to tick off during implementation.

- [ ] AC-1: Description of behavior, observable from outside
- [ ] AC-2: ...
- [ ] AC-3: All scenarios above pass via automated tests
- [ ] AC-4: Lighthouse performance score remains > 90 after this change

## Non-functional requirements specific to this feature

> Performance budget, security considerations, accessibility requirements unique to this feature. Don't repeat the PRD's global NFRs; only call out feature-specific ones.

- ...

## Open questions

> Things to resolve before the spec is `approved`. Once approved, this section should be empty (or moved to plan.md if they're implementation questions).

- [ ] Q1: ...
- [ ] Q2: ...

## Change log

| Status change | Date | Notes |
|---|---|---|
| `draft` | YYYY-MM-DD | Created |
| `approved` | | |
| `in-progress` | | |
| `done` | | |
