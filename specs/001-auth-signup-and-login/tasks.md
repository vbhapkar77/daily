# Tasks: 001 — Auth (signup, login, logout, password reset)

> Ordered list of small, atomic implementation tasks. Each is 1–3 hours of work and independently testable. Each task maps to one PR (or a tight cluster of related commits in one PR).

| | |
|---|---|
| **Status** | `not-started` (approved 2026-06-07, ready to start) |
| **Plan** | [./plan.md](./plan.md) |
| **Spec** | [./spec.md](./spec.md) |

---

## Conventions

- Tasks numbered `T-001`, `T-002`, … in the order they should be done.
- A task should be completable and testable in isolation.
- Each task corresponds to **one PR** (small) or a tightly-related commit cluster.
- Mark `[x]` when merged to `main`.
- The PR title format: `feat(auth): <task subject> (S001 T-NNN)` so traceability is in git log.

---

## Phase A — Backend foundation

These can ship in this order without breaking anything; the temporary permissive `SecurityConfig` keeps the empty backend responsive until T-018.

- [ ] **T-001** Add JJWT dependencies to `pom.xml`
  - File(s): `backend/pom.xml`
  - Add `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (0.12.x)
  - (No Caffeine — per OQ-1, we went DB-backed for rate limiting)
  - Verify `./mvnw -B -q dependency:resolve` succeeds
  - Estimate: 20 min

- [ ] **T-002** Flyway migration V2 — users + password_reset_tokens + rate_limit_events
  - File: `backend/src/main/resources/db/migration/V2__create_users_and_password_reset_tokens.sql`
  - All 3 tables per spec §"Data model changes" (including `rate_limit_events`)
  - Add a smoke test: `@DataJpaTest` that confirms the tables exist after migration
  - Refs: AC-1, AC-6
  - Estimate: 45 min

- [ ] **T-003** `User` entity + `UserRepository` + repo tests
  - File(s): `.../user/User.java`, `.../user/UserRepository.java`, `.../user/UserRepositoryTest.java`
  - Cover `findByEmail` with citext (case-insensitive lookups should work)
  - Estimate: 1 h

- [ ] **T-004** `PasswordResetToken` entity + repository + repo tests
  - File(s): `.../auth/PasswordResetToken.java`, `.../auth/PasswordResetTokenRepository.java`, `.../auth/PasswordResetTokenRepositoryTest.java`
  - Cover `findByTokenHash`, expiry filtering, unused-only filter
  - Estimate: 1 h

- [ ] **T-004b** `RateLimitEvent` entity + repository + repo tests
  - File(s): `.../auth/RateLimitEvent.java`, `.../auth/RateLimitEventRepository.java`, `.../auth/RateLimitEventRepositoryTest.java`
  - Cover `countByBucketKeyAndOccurredAtAfter(key, since)` and `findEarliestInWindow(key, since)`
  - Refs: AC-6
  - Estimate: 1 h

## Phase B — Crypto & utilities

These have no service-layer dependencies. Pure functions, easy to unit-test to 100%.

- [ ] **T-005** `PasswordHasher` utility + tests
  - File(s): `.../lib/auth/PasswordHasher.java`, `PasswordHasherTest.java`
  - Wraps `BCryptPasswordEncoder(12)`. Methods: `hash(plain)`, `verify(plain, hash)`
  - Refs: NFR-S001-SEC-1
  - Estimate: 30 min

- [ ] **T-006** `TokenHasher` (SHA-256 for reset tokens) + tests
  - File(s): `.../lib/auth/TokenHasher.java`, `TokenHasherTest.java`
  - Refs: NFR-S001-SEC-4
  - Estimate: 20 min

- [ ] **T-007** `SecureTokens` random generator + tests
  - File(s): `.../lib/auth/SecureTokens.java`, `SecureTokensTest.java`
  - 32-byte random base64url-encoded; verify ≥ 32 bytes of entropy
  - Estimate: 30 min

- [ ] **T-008** `JwtService` (sign/verify/parse) + tests
  - File(s): `.../lib/auth/JwtService.java`, `JwtServiceTest.java`
  - Sign JWT with HS256 using `JWT_SECRET` from config; payload includes `sub` (userId), `iat`, `exp`
  - Test: tampered tokens fail; expired tokens fail; valid tokens parse
  - Refs: AC-13, NFR-S001-SEC-3
  - Estimate: 2 h

- [ ] **T-009** `RateLimiter` (DB-backed sliding window) + tests
  - File(s): `.../lib/auth/RateLimiter.java`, `RateLimiterTest.java`
  - Methods: `tryAcquire(key, limit, window)` (returns boolean), `recordAttempt(key)`, `secondsUntilReset(key, limit, window)`
  - Implementation: count rows in `rate_limit_events` where `bucket_key = ?` AND `occurred_at >= NOW() - window`; if count < limit, allow + insert a row; else deny
  - Test: window expiry, concurrent inserts (basic concurrency check), cleanup-doesn't-leak
  - Note: integration-style test (uses real Postgres via Testcontainers) rather than pure unit, since the behavior is the SQL
  - Refs: AC-6, OQ-1
  - Estimate: 2.5 h

## Phase C — Domain service layer

- [ ] **T-010** DTOs (records) for all auth request/response shapes
  - File(s): `.../auth/dto/SignupRequest.java`, `LoginRequest.java`, `ForgotRequest.java`, `ResetRequest.java`, `UserDto.java`, `GenericMessage.java`
  - Bean Validation annotations on request DTOs
  - Estimate: 45 min

- [ ] **T-011** Custom exceptions for auth domain
  - File(s): `.../config/exception/AuthExceptions.java` (multiple exception types in one file is fine)
  - `EmailAlreadyRegisteredException`, `InvalidCredentialsException`, `RateLimitedException`, `InvalidResetTokenException`, `UnauthenticatedException`
  - Estimate: 30 min

- [ ] **T-012** `EmailService` with Resend + dev no-op fallback + tests
  - File(s): `.../auth/EmailService.java`, `EmailServiceTest.java`
  - If `RESEND_API_KEY` is empty: log the email body with a clear "[DEV] would have sent" prefix and return success
  - If set: HTTP POST to `https://api.resend.com/emails` with the API key
  - Method signature: `void sendPasswordResetEmail(String toEmail, String resetUrl)`
  - Refs: NFR-S001-OPS-1, ADR-0003
  - Estimate: 2 h

- [ ] **T-013** `AuthService` — signup + login + getCurrentUser + tests
  - File(s): `.../auth/AuthService.java`, `AuthServiceTest.java`
  - Methods: `signup(SignupRequest) → User`, `login(LoginRequest, ipAddress) → User`, `getCurrentUser(userId) → User`
  - Generic error messages on the login fail paths (no enumeration)
  - Refs: AC-1, AC-2, AC-4, AC-5, AC-6, AC-8 (partial — full requires controller)
  - Estimate: 3 h

- [ ] **T-014** `AuthService` — forgot + reset + tests
  - File(s): same `AuthService.java`, expanded
  - Methods: `requestPasswordReset(ForgotRequest, ipAddress)`, `resetPassword(ResetRequest, ipAddress)`
  - Artificial delay on the "no such user" path of forgot (mask timing diff)
  - Refs: AC-9, AC-10, AC-11, AC-12
  - Estimate: 2 h

## Phase D — Web layer

- [ ] **T-015** `AuthController` + web tests
  - File(s): `.../auth/AuthController.java`, `AuthControllerTest.java` (`@WebMvcTest`)
  - All endpoints from `api.yaml`
  - Sets/clears cookies via injected `CookieFactory`
  - Refs: AC-1, AC-3, AC-4, AC-7, AC-8, AC-9, AC-11
  - Estimate: 2 h

- [ ] **T-016** `GlobalExceptionHandler` mapping our exceptions to `Error` schema
  - File: `.../config/GlobalExceptionHandler.java`
  - `@RestControllerAdvice`. Each auth exception → specific HTTP code + error JSON
  - Validation errors → 400 with `details` map (field → list of messages)
  - Refs: AC-2, AC-3, AC-5, AC-12
  - Estimate: 1 h

- [ ] **T-017** `CookieFactory` (env-aware) + tests
  - File(s): `.../config/security/CookieFactory.java`, `CookieFactoryTest.java`
  - Reads `app.cookie.same-site` and `app.cookie.secure` from config
  - Methods: `sessionCookie(jwt)`, `expiredSessionCookie()`
  - Refs: NFR-S001-SEC-3, OQ-5
  - Estimate: 45 min

## Phase E — Security filter chain (the replacement)

- [ ] **T-018** `JwtCookieAuthenticationFilter` + `UserPrincipal`
  - File(s): `.../config/security/JwtCookieAuthenticationFilter.java`, `UserPrincipal.java`
  - Extracts session cookie; validates JWT via `JwtService`; loads `User`; sets `SecurityContext`
  - Skips itself if no cookie (delegates to next filter, which may deny)
  - Estimate: 2 h

- [ ] **T-019** Replace `SecurityConfig` with real Spring Security chain
  - File: `.../config/SecurityConfig.java` (replace temp impl)
  - Stateless; public paths: `/api/v1/auth/{signup,login,forgot,reset}`, `/actuator/health`, `/swagger-ui/**`, `/openapi.yaml`
  - All other paths require authentication
  - Add `JwtCookieAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
  - Keep CORS configuration (already env-driven)
  - Refs: AC-7, AC-9
  - Estimate: 2 h

## Phase F — Integration tests

- [ ] **T-020** `AuthIntegrationTest` covering all spec scenarios end-to-end
  - File: `.../auth/AuthIntegrationTest.java` (`@SpringBootTest` + Testcontainers)
  - One test method per AC where applicable
  - Delete superseded `DailyApplicationTests.java`
  - Refs: AC-1, AC-2, AC-4, AC-5, AC-6, AC-7, AC-8, AC-9, AC-11
  - Estimate: 4 h

## Phase G — OpenAPI merge + backend smoke

- [ ] **T-021** Merge spec 001 `api.yaml` into canonical `docs/api/openapi.yaml`
  - File: `docs/api/openapi.yaml`
  - Re-copy to `backend/src/main/resources/static/openapi.yaml`
  - Verify Spectral lint passes locally and in CI
  - Estimate: 30 min

- [ ] **T-022** Smoke test local backend end-to-end with curl
  - No new files; just verifies T-001..T-021 produced a working backend
  - Curl: signup → me → logout → login → forgot → reset (mock-token-from-logs)
  - Document the sequence as a runbook entry
  - Estimate: 45 min

## Phase H — Frontend

- [ ] **T-023** Frontend deps: Vitest, RTL, MSW, Playwright
  - File: `frontend/package.json`, `frontend/vitest.config.ts`, `frontend/playwright.config.ts`
  - Add scripts: `test`, `test:e2e`, `test:coverage`
  - Estimate: 45 min

- [ ] **T-024** API fetch wrapper + tests
  - File(s): `frontend/src/lib/api.ts`, `frontend/src/lib/api.test.ts`
  - Wraps fetch; sets `credentials: 'include'`; prefixes API_URL; parses JSON; throws typed `ApiError` on non-2xx
  - Estimate: 1 h

- [ ] **T-025** `AuthProvider` + `useAuth` + tests
  - File(s): `frontend/src/lib/auth/AuthProvider.tsx`, `useAuth.ts`, `AuthProvider.test.tsx`
  - Calls `GET /api/v1/auth/me` on mount; stores user; exposes signup/login/logout actions
  - MSW handlers for the test
  - Estimate: 2 h

- [ ] **T-026** `RequireAuth` wrapper component
  - File: `frontend/src/lib/auth/RequireAuth.tsx`
  - Redirects to `/login` if no user (after AuthProvider resolves)
  - Estimate: 30 min

- [ ] **T-027** Auth layout + form components
  - File(s): `frontend/src/app/(auth)/layout.tsx`, `frontend/src/components/AuthForm.tsx`
  - Centered card; logo; consistent shell for signup/login/forgot/reset
  - Estimate: 1.5 h

- [ ] **T-028** Signup page
  - File: `frontend/src/app/(auth)/signup/page.tsx`
  - Refs: AC-1, AC-2, AC-3, AC-14
  - Estimate: 1.5 h

- [ ] **T-029** Login page
  - File: `frontend/src/app/(auth)/login/page.tsx`
  - Includes "Forgot password?" link
  - Refs: AC-4, AC-5
  - Estimate: 1 h

- [ ] **T-030** Forgot password page
  - File: `frontend/src/app/(auth)/forgot/page.tsx`
  - Refs: AC-9, AC-10
  - Estimate: 1 h

- [ ] **T-031** Reset password page
  - File: `frontend/src/app/(auth)/reset/page.tsx`
  - Reads `?token=...`; shows new-password form
  - Refs: AC-11, AC-12
  - Estimate: 1 h

- [ ] **T-032** UserNav component (header signed-in/signed-out states)
  - File: `frontend/src/components/UserNav.tsx`
  - Used in main layout
  - Estimate: 1 h

- [ ] **T-033** Dashboard placeholder + RequireAuth wiring
  - File: `frontend/src/app/dashboard/page.tsx`
  - Shows: "Hi, {user.email} — you're authenticated. Habits coming in spec 002."
  - Refs: AC-8
  - Estimate: 45 min

- [ ] **T-034** Update home page (logged-in redirect, logged-out CTAs)
  - File: `frontend/src/app/page.tsx`, `frontend/src/app/layout.tsx`
  - Wrap layout in `<AuthProvider>`
  - Estimate: 45 min

## Phase I — End-to-end

- [ ] **T-035** Playwright e2e: signup → dashboard → logout → login
  - File: `frontend/e2e/auth.spec.ts`
  - Refs: AC-14, AC-16
  - Estimate: 2 h

## Phase J — Documentation & deploy

- [ ] **T-036** Learning notes (Spring Security, JWT+cookies, bcrypt)
  - File(s): `docs/learnings/spring/security.md`, `auth/jwt-and-cookies.md`, `auth/bcrypt.md`
  - One note per concept hit during implementation; include interview-question framings
  - Estimate: 2 h

- [ ] **T-037** Add `RESEND_API_KEY` to Render env vars; restart backend
  - User action (web UI)
  - Estimate: 10 min

- [ ] **T-038** Push to main; verify auto-deploy on Vercel + Render; smoke-test prod
  - Open https://daily-seven-olive.vercel.app
  - Click "Sign up", create an account, log out, log back in, request a reset, click the email link, set new password
  - Estimate: 30 min

- [ ] **T-039** Update [spec.md](./spec.md) status to `done`; check off all AC boxes; update [tasks.md](./tasks.md) status
  - Estimate: 15 min

---

## Notes / decisions made during implementation

> Capture any decisions made *while* implementing that weren't anticipated in plan.md. These often inform future ADRs or learning notes.

(none yet — will fill in as we work)

---

## Status summary (auto-updated)

- **Total tasks:** 40 (was 39; added T-004b for RateLimitEvent repo)
- **Completed:** 0
- **In progress:** 0
- **Estimated total effort:** ~35 hours
