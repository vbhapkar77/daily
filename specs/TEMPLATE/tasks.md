# Tasks: [Feature name]

> Ordered list of small, atomic implementation tasks. Each is 1–3 hours of work and independently testable.

| | |
|---|---|
| **Status** | `not-started` / `in-progress` / `done` |
| **Plan** | [./plan.md](./plan.md) |

---

## Conventions

- Tasks are numbered `T-001`, `T-002`, ... in the order they should be done.
- A task should be completable and testable in isolation (no half-merged states).
- Each task corresponds to **one PR** (or a tight cluster of related commits if a single PR is more appropriate).
- Mark `[x]` when merged to `main`.

## Backend tasks

- [ ] **T-001** — Add `users` table migration (`V_N__create_users.sql`)
  - File(s): `backend/src/main/resources/db/migration/...`
  - Tests: smoke test that migration applies cleanly (`@DataJpaTest`)
  - Estimate: 30 min

- [ ] **T-002** — Add `User` entity + `UserRepository`
  - File(s): `backend/src/main/java/.../User.java`, `.../UserRepository.java`
  - Tests: repository tests with `@DataJpaTest` + Testcontainers
  - Estimate: 1 h

- [ ] **T-003** — Implement password hashing utility
  - File(s): `backend/src/main/java/.../auth/PasswordHasher.java`
  - Tests: unit tests for hash + verify
  - Estimate: 30 min

- [ ] **T-004** — Implement signup endpoint
  - File(s): `backend/src/main/java/.../AuthController.java`, `.../SignupRequest.java`, `.../AuthService.java`
  - OpenAPI: `POST /api/v1/auth/signup`
  - Tests: web layer test + integration test
  - References: AC-1, AC-2 of [spec.md](./spec.md)
  - Estimate: 1.5 h

## Frontend tasks

- [ ] **T-005** — Generate API types from OpenAPI
  - Command: `pnpm openapi:gen`
  - Verify: new types appear in `frontend/lib/api-types.ts`
  - Estimate: 5 min

- [ ] **T-006** — Build signup form component
  - File(s): `frontend/app/(auth)/signup/page.tsx`, `frontend/components/SignupForm.tsx`
  - Tests: component test with MSW for happy + error paths
  - Estimate: 2 h

## End-to-end

- [ ] **T-007** — Playwright e2e: full signup flow
  - File: `frontend/e2e/signup.spec.ts`
  - Covers: scenarios 1, 2, 3 from spec.md
  - Estimate: 1 h

## Documentation tasks

- [ ] **T-008** — Update `docs/learnings/spring/security.md` with notes from this implementation
- [ ] **T-009** — Update `specs/NNN-.../spec.md` status to `done` once all tests pass

## Notes / decisions made during implementation

> Capture any decisions made *while* implementing that weren't anticipated in plan.md. These often inform future ADRs or learning notes.

- (none yet)
