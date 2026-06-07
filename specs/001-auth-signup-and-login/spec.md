# Spec: 001 — Auth (signup, login, logout, password reset)

| | |
|---|---|
| **Status** | `approved` |
| **Author** | Vishal + Claude |
| **Created** | 2026-06-07 |
| **PRD requirements** | [US-01](../../docs/prd.md#5-user-stories-mvp), [US-15](../../docs/prd.md#5-user-stories-mvp), [F-AUTH-1 through F-AUTH-6](../../docs/prd.md#61-auth) |
| **Relates to ADRs** | [ADR-0001](../../docs/decisions/0001-auth-email-password.md), [ADR-0003](../../docs/decisions/0003-email-provider-resend.md), [ADR-0007](../../docs/decisions/0007-testing-strategy.md), [ADR-0010](../../docs/decisions/0010-adopt-sdd-practices.md) |
| **Related specs** | — (this is the first feature spec) |

---

## Goal

Implement DIY email + password authentication so users can create accounts, sign in, sign out, and recover forgotten passwords via email. Replaces the temporary permissive `SecurityConfig` in the scaffold with a real Spring Security filter chain backed by JWT-in-cookie sessions, bcrypt-hashed passwords, and Resend-delivered transactional email.

This is the **foundational feature** — every other feature in the app (habits, check-ins, captures) is gated behind authentication.

---

## User scenarios (Given / When / Then)

### Scenario 1: Successful signup (happy path)

```
Given an anonymous visitor is on the signup page
When they enter a valid email (e.g., "alice@example.com")
  And a password of 8+ characters (e.g., "correcthorse")
  And submit the form
Then a `users` row is created with their bcrypt-hashed password
  And a JWT session cookie is set on their browser (HttpOnly, Secure, 30-day TTL)
  And they are redirected to the home / dashboard
  And the page now shows their email in the header
```

### Scenario 2: Signup with an existing email

```
Given an anonymous visitor is on the signup page
  And a user already exists with email "alice@example.com"
When they try to sign up with "alice@example.com"
Then the request returns HTTP 409 with error code "email-already-registered"
  And no second user row is created
  And no session cookie is set
  And the form displays a clear error: "An account with this email already exists. Try logging in instead."
```

### Scenario 3: Signup with weak password

```
Given an anonymous visitor is on the signup page
When they submit a password shorter than 8 characters
Then the request returns HTTP 400 with error code "validation-failed"
  And the response body includes `details: { password: ["must be at least 8 characters"] }`
  And the form highlights the password field with the error
  And no user row is created
```

### Scenario 4: Successful login

```
Given a user exists with email "alice@example.com" and password "correcthorse"
When they submit the login form with those credentials
Then the request returns HTTP 200 with the user object (id, email, name, timezone)
  And a fresh JWT session cookie is set (HttpOnly, Secure, 30-day TTL)
  And the frontend redirects them to the dashboard
```

### Scenario 5: Login with wrong password

```
Given a user exists with email "alice@example.com"
When they submit login with the right email but wrong password
Then the request returns HTTP 401 with error code "invalid-credentials"
  And the error message is the generic "Invalid email or password."
  And NO information is leaked about whether the email exists in the system
  And no session cookie is set
```

### Scenario 6: Login with non-existent email

```
Given no user exists with email "bob@example.com"
When someone submits login with "bob@example.com" and any password
Then the request returns HTTP 401 with the SAME generic message as Scenario 5
  (preventing user enumeration)
```

### Scenario 7: Login rate-limited

```
Given a user (or attacker) has made 5 failed login attempts for "alice@example.com" within 15 minutes
When they make a 6th attempt
Then the request returns HTTP 429 with error code "too-many-attempts"
  And the response includes a Retry-After header
  And the message is: "Too many login attempts. Please try again in N minutes."
  And the lockout lasts 15 minutes from the 5th failed attempt
```

### Scenario 8: Successful logout

```
Given a logged-in user
When they POST /api/v1/auth/logout
Then the session cookie is cleared (Set-Cookie with Max-Age=0)
  And subsequent requests have no auth context
  And the frontend redirects them to the home page
```

### Scenario 9: Accessing protected endpoint without session

```
Given an anonymous user (no session cookie)
When they call any /api/v1/* endpoint other than /api/v1/auth/{signup,login,forgot,reset}
Then the request returns HTTP 401 with error code "unauthorized"
  And the response message is "Authentication required."
```

### Scenario 10: GET current user

```
Given a logged-in user
When they call GET /api/v1/auth/me
Then the request returns HTTP 200 with their user object
  And the response excludes the password_hash and any other sensitive fields
```

### Scenario 11: Forgot password — email exists

```
Given a user with email "alice@example.com" exists
When they POST /api/v1/auth/forgot with that email
Then the request returns HTTP 200 with a generic success message
  And a row is inserted in `password_reset_tokens` (32-byte random token, hashed with SHA-256, expires_at = NOW() + 1 hour)
  And an email is sent via Resend containing the plaintext token in a reset URL
  And the URL format is `{APP_BASE_URL}/reset?token=<plaintext>`
```

### Scenario 12: Forgot password — email doesn't exist

```
Given no user with email "ghost@example.com" exists
When someone POSTs /api/v1/auth/forgot with that email
Then the request returns HTTP 200 with the SAME generic message as Scenario 11
  (preventing user enumeration)
  And NO database insert or email send happens
  And the response is artificially delayed by ~100-300ms to mask the timing difference
```

### Scenario 13: Reset password with valid token

```
Given a password_reset_tokens row exists with token_hash = sha256("plain"), not expired, not used
When the user POSTs /api/v1/auth/reset with `{ token: "plain", new_password: "newcorrecthorse" }`
Then the user's password_hash is updated to bcrypt("newcorrecthorse", cost=12)
  And the password_reset_tokens row is marked `used_at = NOW()`
  And the request returns HTTP 200 with a success message
  And the user must explicitly log in again with the new password (no auto-login)
```

### Scenario 14: Reset with expired token

```
Given a password_reset_tokens row with expires_at < NOW()
When the user POSTs /api/v1/auth/reset with that token
Then the request returns HTTP 400 with error code "invalid-reset-token"
  And the generic message is "This reset link is invalid or has expired. Request a new one."
  (Same message as scenarios 15 and 16 — no information leak)
  And the password is NOT updated
```

### Scenario 15: Reset with already-used token

Same as Scenario 14 but for a token where `used_at IS NOT NULL`.

### Scenario 16: Reset with invalid/forged token

Same as Scenario 14 but for a token where no matching row exists.

---

## Data model changes

Two new tables, added via Flyway migration `V2__create_users_and_password_reset_tokens.sql`:

```sql
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         CITEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    name          TEXT,
    timezone      TEXT NOT NULL DEFAULT 'UTC',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);   -- citext already case-insensitive
CREATE INDEX idx_users_created_at ON users (created_at DESC);

CREATE TABLE password_reset_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,        -- SHA-256 of the plain token; plain never stored
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prt_user_id ON password_reset_tokens (user_id);
CREATE INDEX idx_prt_expires_at ON password_reset_tokens (expires_at);

-- DB-backed sliding-window rate limiter (per OQ-1 resolution).
-- One row per attempted operation that is rate-counted. We only insert FAILED
-- login attempts (successful logins reset the bucket conceptually because they
-- prove the user is legit). For signup/forgot/reset, we insert every attempt.
CREATE TABLE rate_limit_events (
    id          BIGSERIAL PRIMARY KEY,
    bucket_key  TEXT NOT NULL,           -- e.g. "login:alice@x.com:1.2.3.4", "signup:1.2.3.4"
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rle_bucket_occurred ON rate_limit_events (bucket_key, occurred_at DESC);
```

We rely on the `citext` extension (already enabled in `V1__init.sql`) for case-insensitive email matching.

A future maintenance migration (out of scope for this spec) will add a scheduled cleanup of `rate_limit_events` older than 24 hours — for v1 we let them accumulate (Postgres handles millions of rows in this size easily).

---

## API additions

Full OpenAPI fragment lives in [`api.yaml`](api.yaml) in this folder. Summary:

| Method | Path | Body | Auth | Rate-limited |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/signup` | `{ email, password, name? }` | none | yes (10/min/IP) |
| `POST` | `/api/v1/auth/login` | `{ email, password }` | none | yes (5/15min/(email+IP)) |
| `POST` | `/api/v1/auth/logout` | — | required | no |
| `POST` | `/api/v1/auth/forgot` | `{ email }` | none | yes (3/hour/IP) |
| `POST` | `/api/v1/auth/reset` | `{ token, new_password }` | none | yes (10/hour/IP) |
| `GET` | `/api/v1/auth/me` | — | required | no |

All non-auth endpoints (when they exist in future specs) require a valid session cookie or return 401.

---

## UI changes

New pages added in `frontend/src/app/`:

| Path | Purpose |
|---|---|
| `/signup` | Email + password + (optional) name form |
| `/login` | Email + password form, with "forgot password?" link |
| `/forgot` | Email-only form; always shows generic success message |
| `/reset` | Reads `?token=...` from URL, shows new-password form |
| `/dashboard` | Placeholder protected page — proves the auth gate works |

New components:
- `AuthForm` — shared shell for signup/login (input fields, submit button, error display, link to the other action)
- `UserNav` — header element showing email and a logout button when logged in; sign-in/sign-up links when logged out
- `AuthProvider` — React context wrapping the app, exposes `useUser()` and `useAuth()` hooks. Calls `GET /api/v1/auth/me` on mount.
- `RequireAuth` — wrapper component that redirects to `/login` if `useUser()` returns null

The existing landing page at `/` is updated:
- If logged out: shows the marketing intro + "Sign in" and "Get started" CTAs
- If logged in: redirects to `/dashboard`

The `HealthBadge` component stays (useful for ops sanity).

---

## Out of scope (explicitly)

| Item | Why deferred |
|---|---|
| Email verification on signup | PRD F-AUTH-7 explicitly defers to v1.1 |
| OAuth (Google / GitHub sign-in) | ADR-0001 — possible to add in v1.x without schema breakage |
| Multi-factor authentication (MFA) | Not in PRD; future enhancement |
| Email change / account settings | Out of scope; v2 |
| Account deletion | PRD NFR-SEC-7 mentions but accepts "manual support" for v1 |
| Remember-me checkbox / configurable TTL | 30-day fixed cookie is fine for v1 |
| CAPTCHA on signup / forgot | Rate limiting is sufficient at our scale |
| Username (separate from email) | Email is the identifier in v1 |
| Password strength meter (frontend) | Min-8-chars check is sufficient v1 |
| Password breach check (HIBP API) | Future improvement |
| Audit log of auth events | Future improvement |

---

## Acceptance criteria

Each AC maps to one or more tests. Numbered for traceability.

- [ ] **AC-1** A POST to `/api/v1/auth/signup` with `{ email, password (≥8 chars), name? }` creates a `users` row with a bcrypt(12) password hash and sets a JWT cookie.
- [ ] **AC-2** A signup with an email that already exists returns HTTP 409, code `email-already-registered`. No duplicate row created.
- [ ] **AC-3** A signup with password < 8 chars returns HTTP 400 with `validation-failed` and field-level errors.
- [ ] **AC-4** A POST to `/api/v1/auth/login` with correct credentials returns the User and sets a fresh JWT cookie.
- [ ] **AC-5** Login with wrong password OR non-existent email returns HTTP 401 with the **same** generic message: "Invalid email or password." (No user enumeration.)
- [ ] **AC-6** After 5 failed login attempts for an email within 15 minutes, the 6th returns HTTP 429 with `Retry-After` header.
- [ ] **AC-7** POST to `/api/v1/auth/logout` clears the cookie. Subsequent requests are unauthenticated.
- [ ] **AC-8** GET `/api/v1/auth/me` returns the current user (without `password_hash`) for authenticated requests, 401 otherwise.
- [ ] **AC-9** POST `/api/v1/auth/forgot` with **any** email returns HTTP 200 with the same generic message. If the email exists, a token is created and an email is sent. Otherwise, no DB or email side effect.
- [ ] **AC-10** Forgot-password emails contain a link of the form `{APP_BASE_URL}/reset?token=<32+ chars>`.
- [ ] **AC-11** POST `/api/v1/auth/reset` with a valid, unexpired, unused token replaces the password_hash and marks the token used.
- [ ] **AC-12** POST `/api/v1/auth/reset` with an expired, used, or unknown token returns HTTP 400 with the same generic message: "This reset link is invalid or has expired. Request a new one."
- [ ] **AC-13** Sessions persist for at least 30 days. Logging out before then immediately invalidates the cookie.
- [ ] **AC-14** Signup, login, and password reset workflows complete in the frontend without page reload (SPA-style).
- [ ] **AC-15** A user can sign up on their laptop and log into the same account from their phone using the same email + password.
- [ ] **AC-16** A Playwright e2e test exercises Scenario 1 (signup), Scenario 4 (login), and Scenario 8 (logout) against a real backend.

---

## Non-functional requirements specific to this feature

| ID | Requirement |
|---|---|
| **NFR-S001-PERF-1** | Signup p95 < 800ms (bcrypt is intentionally slow; cost 12 takes ~250ms) |
| **NFR-S001-PERF-2** | Login p95 < 800ms (same reason) |
| **NFR-S001-PERF-3** | All other auth endpoints p95 < 200ms |
| **NFR-S001-SEC-1** | All passwords stored as bcrypt hashes with cost factor 12. NEVER logged. |
| **NFR-S001-SEC-2** | All error messages on the auth path use generic wording for the failure paths to prevent user enumeration. |
| **NFR-S001-SEC-3** | Session cookies are `HttpOnly`, `Secure`, `SameSite=Lax`, signed JWT with HS256 (32+ byte secret). |
| **NFR-S001-SEC-4** | Reset tokens are 32+ bytes from `SecureRandom`, stored hashed (SHA-256). The plaintext token is only known to the user. |
| **NFR-S001-OPS-1** | When `RESEND_API_KEY` is empty (dev), password-reset "emails" are written to the application log instead of sent. |
| **NFR-S001-COVERAGE** | `lib/auth/*` package (or whatever we end up naming the auth business logic) has ≥ 95% line and branch coverage. |

---

## Open questions — RESOLVED at approval

| # | Question | Resolution | Reasoning |
|---|---|---|---|
| **OQ-1** | Rate limiter storage | **DB-backed** (`rate_limit_events` table, sliding window) | More production-grade than in-memory; survives restarts; works in multi-instance setups; matches the standard pattern (Redis is the next step up). Costs ~1-5ms per auth attempt — acceptable. |
| **OQ-2** | Login rate-limit key | **`email + IP` combined** for login; **`IP` only** for signup/forgot/reset | Balanced — attackers can't lock out an email globally; legitimate users on shared NAT aren't blocked. |
| **OQ-3** | Email sender | **Resend dev sender (`onboarding@resend.dev`) in v1**, with explicit TODO to verify own domain in v1.1 | Vishal doesn't currently own a domain; buying one is out of scope for this spec. Spam risk is acceptable for v1 test audience (Vishal + a few friends). Documented in [docs/discussions/2026-06-07-resend-sender-domain.md] (to be created). |
| **OQ-4** | Autocreate default habits on first signup | **No** — separate concern, deferred to spec 002 (Habits CRUD). Empty post-signup state is fine. | Keeps this spec focused. The empty dashboard with an "Add your first habit" prompt is a clean MVP. |
| **OQ-5** | Cookie SameSite policy | **`SameSite=None; Secure`** in prod (cross-origin Vercel → Render); **`SameSite=Lax`** in local dev (same-host via localhost) | Required for the cookie to cross origin in browsers; HTTPS-only in prod (already enforced by Render). |

---

## Change log

| Status change | Date | Notes |
|---|---|---|
| `draft` | 2026-06-07 | Created |
| `approved` | 2026-06-07 | All 5 OQs resolved with Vishal. Locked. |
| `in-progress` | | |
| `done` | | |
