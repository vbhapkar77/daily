# Plan: 001 — Auth (signup, login, logout, password reset)

> The technical approach for implementing [spec.md](./spec.md). Drafted alongside the spec; locks when both are approved together.

| | |
|---|---|
| **Status** | `approved` |
| **Author** | Vishal + Claude |
| **Created** | 2026-06-07 |
| **Spec** | [./spec.md](./spec.md) |

---

## Approach summary

We implement classical email + password auth with Spring Security 6 on the backend and a React Context + protected-route pattern on the frontend. The Spring Security filter chain validates JWTs from a session cookie on every protected request and populates a `SecurityContext` with the authenticated `UserPrincipal`. Bcrypt (cost 12) hashes passwords; JJWT signs/verifies JWTs with HS256; a **DB-backed sliding-window rate limiter** (per OQ-1) gates the auth endpoints by inserting rows into `rate_limit_events`; Resend (or a no-op dev fallback) sends password-reset emails. The temporary permissive `SecurityConfig` we shipped in Phase 2 is fully replaced.

On the frontend, an `AuthProvider` calls `GET /api/v1/auth/me` on mount and stores the user (or `null`) in context. Pages either render based on that user state or use a `RequireAuth` wrapper that redirects to `/login` when the user is missing. All fetches go through a small wrapper that sets `credentials: 'include'` so cookies cross the origin boundary correctly.

---

## Files affected

### Backend

**Migrations**

| Action | Path | Purpose |
|---|---|---|
| create | `backend/src/main/resources/db/migration/V2__create_users_and_password_reset_tokens.sql` | Flyway migration per spec §"Data model changes" — includes `users`, `password_reset_tokens`, AND `rate_limit_events` tables |

**Domain / persistence**

| Action | Path | Purpose |
|---|---|---|
| create | `.../daily/user/User.java` | `@Entity` mirroring `users` table |
| create | `.../daily/user/UserRepository.java` | `JpaRepository<User, Long>` + `findByEmail(String)` |
| create | `.../daily/auth/PasswordResetToken.java` | `@Entity` mirroring `password_reset_tokens` |
| create | `.../daily/auth/PasswordResetTokenRepository.java` | `JpaRepository<PasswordResetToken, Long>` + `findByTokenHash(String)` |
| create | `.../daily/auth/RateLimitEvent.java` | `@Entity` mirroring `rate_limit_events` |
| create | `.../daily/auth/RateLimitEventRepository.java` | `JpaRepository<RateLimitEvent, Long>` + `countByBucketKeyAndOccurredAtAfter(String, Instant)` + `findEarliestInWindow(String, Instant)` |

**Crypto / utilities (`lib/`)**

| Action | Path | Purpose |
|---|---|---|
| create | `.../daily/lib/auth/PasswordHasher.java` | Thin wrapper over `BCryptPasswordEncoder(12)` — easy to test |
| create | `.../daily/lib/auth/TokenHasher.java` | `sha256(plain) → hex` helper for reset tokens |
| create | `.../daily/lib/auth/JwtService.java` | Signs/verifies HS256 JWTs from `JWT_SECRET` env. Issues 30-day tokens with `sub`=userId. |
| create | `.../daily/lib/auth/RateLimiter.java` | DB-backed sliding window. Methods: `tryAcquire(key, limit, window)`, `recordAttempt(key)`, `secondsUntilReset(key, limit, window)`. Reads/writes `rate_limit_events` via `RateLimitEventRepository`. |
| create | `.../daily/lib/auth/SecureTokens.java` | `SecureRandom`-based base64url token generator |

**Service / business logic**

| Action | Path | Purpose |
|---|---|---|
| create | `.../daily/auth/AuthService.java` | `signup`, `login`, `logout`, `requestPasswordReset`, `resetPassword`, `getCurrentUser` |
| create | `.../daily/auth/EmailService.java` | Resend HTTP client + dev no-op fallback (logs the URL instead of sending) |

**Web / DTOs / controllers**

| Action | Path | Purpose |
|---|---|---|
| create | `.../daily/auth/dto/SignupRequest.java` | Record with `@Valid` annotations |
| create | `.../daily/auth/dto/LoginRequest.java` | Record |
| create | `.../daily/auth/dto/ForgotRequest.java` | Record |
| create | `.../daily/auth/dto/ResetRequest.java` | Record |
| create | `.../daily/auth/dto/UserDto.java` | Record: id, email, name, timezone, createdAt (no password_hash) |
| create | `.../daily/auth/dto/GenericMessage.java` | Record: message |
| create | `.../daily/auth/AuthController.java` | All `/api/v1/auth/*` endpoints |
| create | `.../daily/config/GlobalExceptionHandler.java` | `@RestControllerAdvice` mapping our exceptions to the `Error` schema |
| create | `.../daily/config/exception/AuthExceptions.java` | Domain exception types (InvalidCredentials, EmailAlreadyRegistered, etc.) |

**Security**

| Action | Path | Purpose |
|---|---|---|
| MODIFY | `.../daily/config/SecurityConfig.java` | Replace the temporary permissive bean. New chain: stateless, public auth endpoints + actuator/health + swagger, everything else authenticated. JWT cookie filter runs before `UsernamePasswordAuthenticationFilter`. |
| create | `.../daily/config/security/JwtCookieAuthenticationFilter.java` | Extracts session cookie, validates JWT, loads user, sets `SecurityContext` |
| create | `.../daily/config/security/UserPrincipal.java` | Wraps `User` for the `SecurityContext` |
| create | `.../daily/config/security/CookieFactory.java` | Centralizes cookie creation (HttpOnly, Secure, SameSite, Max-Age, env-aware) |

**Tests**

| Action | Path | Purpose |
|---|---|---|
| create | `.../daily/user/UserRepositoryTest.java` | `@DataJpaTest` + Testcontainers; covers findByEmail with citext |
| create | `.../daily/auth/PasswordResetTokenRepositoryTest.java` | `@DataJpaTest` |
| create | `.../daily/lib/auth/PasswordHasherTest.java` | Unit; hash + verify happy paths and bad cases |
| create | `.../daily/lib/auth/TokenHasherTest.java` | Unit |
| create | `.../daily/lib/auth/JwtServiceTest.java` | Unit; sign + verify, expiry, tampered tokens |
| create | `.../daily/lib/auth/RateLimiterTest.java` | Unit; bucket math, window expiry |
| create | `.../daily/auth/AuthServiceTest.java` | Unit with mocked repos; covers all 16 scenarios from spec |
| create | `.../daily/auth/AuthControllerTest.java` | `@WebMvcTest` + MockMvc; covers HTTP layer + validation |
| create | `.../daily/auth/AuthIntegrationTest.java` | `@SpringBootTest` + Testcontainers; full HTTP → DB. Re-enables what's in `DailyApplicationTests` and supersedes it. |

**Configuration**

| Action | Path | Purpose |
|---|---|---|
| modify | `backend/pom.xml` | Add `jjwt-api`/`jjwt-impl`/`jjwt-jackson`, `commons-codec` (sha256). NOTE: no Caffeine (per OQ-1 we went DB-backed). |
| modify | `backend/src/main/resources/application.yml` | Add `app.cookie.same-site`, `app.cookie.secure`, `app.email.from`, `app.auth.bcrypt-cost` defaults |
| modify | `backend/src/main/resources/application-local.yml` | `same-site: Lax`, `secure: false` for HTTP dev |
| modify | `backend/src/main/resources/application-prod.yml` | `same-site: None`, `secure: true`; reads `RESEND_API_KEY` |
| delete | `backend/src/test/java/.../DailyApplicationTests.java` | Superseded by `AuthIntegrationTest` (which actually exercises the boot chain end-to-end) |

### Frontend

**Auth plumbing**

| Action | Path | Purpose |
|---|---|---|
| create | `frontend/src/lib/api.ts` | Thin fetch wrapper: prefixes API_URL, sets `credentials: 'include'`, parses JSON, throws typed errors |
| create | `frontend/src/lib/auth/AuthProvider.tsx` | Context provider calling `GET /api/v1/auth/me` on mount; exposes `user`, `signup`, `login`, `logout` |
| create | `frontend/src/lib/auth/useAuth.ts` | `useAuth()` hook reading from context |
| create | `frontend/src/lib/auth/RequireAuth.tsx` | Wrapper: redirects to `/login` if no user |

**Pages**

| Action | Path | Purpose |
|---|---|---|
| modify | `frontend/src/app/layout.tsx` | Wrap app in `<AuthProvider>` |
| modify | `frontend/src/app/page.tsx` | If logged in → redirect to `/dashboard`; if not → show marketing intro + sign-in/sign-up CTAs |
| create | `frontend/src/app/(auth)/signup/page.tsx` | Signup form |
| create | `frontend/src/app/(auth)/login/page.tsx` | Login form |
| create | `frontend/src/app/(auth)/forgot/page.tsx` | Forgot password form |
| create | `frontend/src/app/(auth)/reset/page.tsx` | Reset password form (reads `?token=...`) |
| create | `frontend/src/app/(auth)/layout.tsx` | Shared layout for the auth pages (centered card, logo) |
| create | `frontend/src/app/dashboard/page.tsx` | Placeholder protected page; uses `RequireAuth` |

**Components**

| Action | Path | Purpose |
|---|---|---|
| create | `frontend/src/components/AuthForm.tsx` | Shared shell: input fields, submit button, error display, link-to-other-action |
| create | `frontend/src/components/UserNav.tsx` | Header: user email + logout button when logged in; sign-in/sign-up when not |
| modify | `frontend/src/components/HealthBadge.tsx` | (Possibly) move to dashboard, or keep on home page footer |

**Tests**

| Action | Path | Purpose |
|---|---|---|
| create | `frontend/src/lib/api.test.ts` | Unit |
| create | `frontend/src/lib/auth/AuthProvider.test.tsx` | Component test with MSW for `/api/v1/auth/me` |
| create | `frontend/e2e/auth.spec.ts` | Playwright e2e: signup → see dashboard → logout → sign back in |

**Configuration**

| Action | Path | Purpose |
|---|---|---|
| modify | `frontend/package.json` | Add `@playwright/test`, `msw`, `vitest`, `@testing-library/react` |
| create | `frontend/vitest.config.ts` | Vitest config (jsdom env, coverage thresholds) |
| create | `frontend/playwright.config.ts` | Playwright config (chromium + webkit, base URL) |
| create | `frontend/src/tests/mocks/handlers.ts` | MSW handlers for `/api/v1/auth/*` |

### Other

| Action | Path | Purpose |
|---|---|---|
| merge | `docs/api/openapi.yaml` | Add the paths and schemas from [api.yaml](./api.yaml) |
| modify | `backend/src/main/resources/static/openapi.yaml` | Re-copy the merged spec (built artifact) |
| create | `docs/learnings/spring/security.md` | Notes on Spring Security filter chain, why we use stateless config, common interview Qs |
| create | `docs/learnings/auth/jwt-and-cookies.md` | When to use cookies vs Authorization header, SameSite explained, XSS vs CSRF tradeoffs |
| create | `docs/learnings/auth/bcrypt.md` | Why bcrypt, cost factor explanation, common alternatives (argon2, scrypt) |
| modify | `AGENTS.md` | Add the new conventions surfaced (e.g., "always filter queries by userId at service layer") |
| modify | `backend/.env.example` | Add `RESEND_API_KEY` and `CORS_ALLOWED_ORIGINS` reminders |

---

## Dependencies on other work

- Spec 001 has **no prerequisites** beyond Phase 2.5 being shipped (which it is).
- It is a **prerequisite for** Spec 002 (Habits CRUD) and every subsequent feature.

External deps to add:
- `io.jsonwebtoken:jjwt-api:0.12.x` (+ impl + jackson)
- Resend SDK — we'll use a plain `RestTemplate`/`WebClient` since their Java SDK is immature; the API surface is one POST. Avoids a dep.

Account / config:
- **Resend account** — sign up at https://resend.com (free tier, 100 emails/day). Generate an API key. **You'll do this manually**, then I add `RESEND_API_KEY` to Render env vars.
- **`CORS_ALLOWED_ORIGINS`** on Render is already set; we just need to keep it current as new origins (e.g., custom domain) are added.

---

## Risks & mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Cookie does not cross origin (frontend on vercel.app, backend on onrender.com) due to SameSite policy | High | High | Use `SameSite=None; Secure` in prod profile; always `credentials: 'include'` in frontend fetch; document in [docs/learnings/auth/jwt-and-cookies.md](../../docs/learnings/auth/jwt-and-cookies.md) |
| Spring Security misconfig accidentally locks us out of the app | Medium | High | Comprehensive integration tests; deploy to a preview env first (we don't have one yet, but smoke-test on staging Render service if we add one) |
| User enumeration via timing (forgot-password endpoint takes different time for existing vs non-existing email) | Medium | Medium | Artificial delay 100-300ms in the "no user" path; document |
| JWT secret leak — anyone with it can mint sessions | Low | Critical | Never log; never commit; rotate quarterly; secret is 48-byte from `openssl rand -base64 48` |
| bcrypt cost 12 is slow → DOS risk if attacker spams login | Medium | Medium | Rate limiter (5/15min/email+IP) already required for correctness; doubles as DOS mitigation |
| Resend free tier (100/day, 3000/month) exhausted by spam | Low | Medium | IP-based rate limit on /forgot (3/hour); monitor monthly volume |
| Token in URL gets leaked via Referer header or browser history | Medium | Medium | One-time use; 1-hour expiry; in production: would set `Referrer-Policy: no-referrer` header on the `/reset` page (deferred to NFR sweep) |
| Test isolation: parallel integration tests share Postgres state | Medium | Medium | `@Transactional` rollback per test (default for `@DataJpaTest`); explicit cleanup in `@AfterEach` for full-context tests |

---

## Testing strategy for this feature

Each AC from spec.md maps to one or more tests. Coverage gate: ≥95% on `lib/auth/*` and `daily/auth/*` packages.

| AC | Test layer | Test file(s) |
|---|---|---|
| AC-1 | Integration | `AuthIntegrationTest.signupHappyPath()` |
| AC-1 | Unit | `AuthServiceTest.signupCreatesUserAndIssuesToken()` |
| AC-2 | Integration | `AuthIntegrationTest.signupRejectsDuplicateEmail()` |
| AC-3 | Web (MockMvc) | `AuthControllerTest.signupValidationFailsOnShortPassword()` |
| AC-4 | Integration | `AuthIntegrationTest.loginHappyPath()` |
| AC-5 | Integration | `AuthIntegrationTest.loginGenericErrorOnWrongPassword()` + `loginGenericErrorOnUnknownEmail()` (assert SAME response) |
| AC-6 | Integration | `AuthIntegrationTest.loginRateLimitsAfterFiveFailures()` |
| AC-7 | Integration | `AuthIntegrationTest.logoutClearsCookie()` |
| AC-8 | Integration | `AuthIntegrationTest.meReturnsUserWhenAuth()` + `meReturns401WhenAnon()` |
| AC-9 | Integration | `AuthIntegrationTest.forgotReturnsGenericForBothExistingAndUnknown()` |
| AC-10 | Unit | `AuthServiceTest.forgotEmailContainsResetLink()` (with mocked EmailService capturing the email body) |
| AC-11 | Integration | `AuthIntegrationTest.resetWithValidTokenUpdatesPassword()` |
| AC-12 | Unit | `AuthServiceTest.resetGenericErrorOnExpiredToken()` + `resetGenericErrorOnUsedToken()` + `resetGenericErrorOnUnknownToken()` |
| AC-13 | Unit | `JwtServiceTest.tokenExpiresAfter30Days()` |
| AC-14 | E2E | `auth.spec.ts` — full flow without page reload |
| AC-15 | E2E | Manual (use phone + laptop simultaneously); not automated in v1 |
| AC-16 | E2E | `auth.spec.ts` (one Playwright spec covers AC-14 and AC-16) |

---

## Rollout / migration

This is the first feature post-launch — there are no existing users to migrate. Once merged:
- Flyway runs `V2__create_users_and_password_reset_tokens.sql` automatically on backend startup
- The new `SecurityConfig` immediately replaces the permissive one — any anonymous traffic to non-auth endpoints starts getting 401
- The `HealthBadge` continues working (health endpoint stays public)

No feature flag — auth is foundational; either it works or the app is unusable.

---

## Estimated effort

Rough breakdown (will tune as we go):

- Backend domain + persistence + repositories: ~3 h
- Crypto utilities + JWT + rate limiter: ~3 h
- AuthService + EmailService + DTOs: ~4 h
- SecurityConfig + JwtCookieFilter + exception handling: ~4 h
- AuthController + web tests: ~2 h
- Backend integration tests (covers most ACs): ~4 h
- Frontend AuthProvider + RequireAuth + api wrapper: ~3 h
- Frontend pages (signup/login/forgot/reset/dashboard): ~4 h
- Frontend tests (Vitest + MSW + Playwright): ~3 h
- Documentation (learnings notes, AGENTS.md updates): ~2 h
- OpenAPI merge + verify Spectral passes: ~1 h
- Deploy + smoke-test in prod: ~1 h

**Total: ~34 hours of focused work**, spread across as many sessions as needed. Breaks down into ~25 atomic tasks in [tasks.md](./tasks.md).

---

## Change log

| Status change | Date | Notes |
|---|---|---|
| `draft` | 2026-06-07 | Created alongside spec.md |
| `approved` | 2026-06-07 | OQs resolved; swapped Caffeine → DB-backed rate limiter; locked |
