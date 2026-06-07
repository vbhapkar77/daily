# Plan: [Feature name]

> The technical approach for implementing the spec. Written *after* spec.md is approved.

| | |
|---|---|
| **Status** | `draft` / `approved` |
| **Author** | Vishal |
| **Created** | YYYY-MM-DD |
| **Spec** | [./spec.md](./spec.md) |

---

## Approach summary

> Two paragraphs explaining the technical approach end-to-end. The "how" that matches the spec's "what."

## Files affected

> List of files to be created or modified. Use this for sizing and for ensuring nothing critical is missed during implementation.

### Backend

| Action | Path | Purpose |
|---|---|---|
| create | `backend/src/main/java/...` | ... |
| modify | `backend/src/main/java/...` | ... |
| create | `backend/src/main/resources/db/migration/V_N__...sql` | DB migration |
| create | `backend/src/test/java/...` | tests |

### Frontend

| Action | Path | Purpose |
|---|---|---|
| create | `frontend/app/.../page.tsx` | new page |
| create | `frontend/components/...` | ... |
| modify | `frontend/lib/api.ts` | use new generated types |

### Other

| Action | Path | Purpose |
|---|---|---|
| modify | `docs/api/openapi.yaml` | merge in this feature's `api.yaml` |
| modify | `AGENTS.md` if conventions changed | |

## Dependencies on other work

> Specs or external deps that must be in place before this can be implemented.

- Spec `00X-...` must be `done` (provides the foundation)
- Library `xyz` must be added to `pom.xml` / `package.json`
- Env var `SOMETHING` must be added to dev + production

If none: write "No prerequisites."

## Risks & mitigations

> Things that could go wrong during implementation, and how we'll handle them.

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| ... | Low/Med/High | Low/Med/High | ... |

## Testing strategy for this feature

> Which test layers cover what acceptance criterion? Reference the AC IDs from spec.md.

| Acceptance criterion | Test layer | Test file |
|---|---|---|
| AC-1 | unit | `lib/...Test.java` |
| AC-2 | integration | `...IntegrationTest.java` |
| AC-3 | e2e | `e2e/...spec.ts` |

## Rollout / migration

> If this is shipped to a system already in production: how do existing users / data migrate? Backward compatibility? Feature flag?

For v1 (no users yet): typically "N/A — pre-launch, no migration concerns."

## Estimated effort

> Rough time estimate. Useful for planning and for retrospective comparison.

- ~X hours / Y tasks

## Change log

| Status change | Date | Notes |
|---|---|---|
| `draft` | YYYY-MM-DD | Created after spec.md approved |
| `approved` | | |
