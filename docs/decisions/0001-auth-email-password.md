# ADR-0001 — Use email + password authentication

**Status:** Accepted
**Date:** 2026-06-07
**Decision makers:** Vishal Bhapkar
**Relates to:** [PRD §6.1 F-AUTH-*](../prd.md#61-auth)

---

## Context

The PRD requires user accounts (multi-user from day one). Auth strategy needs to be chosen before any code is written because it affects:
- The `users` table schema
- The session mechanism
- Which API routes need protection
- The signup/login UI flow
- Email infrastructure requirements (forgot-password, verification)

The product is positioned for *any* self-motivated user, not just developers. The primary persona explicitly includes non-technical knowledge workers — meaning we cannot assume every user has a GitHub account.

Vishal also stated a strong preference for a "real production app" — one he could direct any user (friend, family, prospect) to sign up for without explanation.

## Decision

We will implement **classical email + password authentication** as the sole auth method for v1.

The implementation includes:
- Signup form: email, password (min 8 chars), name (optional)
- Login form: email, password
- Forgot-password flow: user enters email → system emails a single-use, 1-hour-valid reset token → user clicks link → sets new password
- Passwords are hashed with **bcrypt** (cost factor 12) before storage. Plaintext passwords never touch persistent storage or logs.
- Session = signed JWT in HttpOnly, Secure, SameSite=Lax cookie; 30-day TTL
- Rate limiting: max 5 failed login attempts per (email + IP) per 15-minute window; lockout 15 min on breach

Email verification on signup is **deferred to v1.1** — in v1, accounts are usable immediately upon signup. This is a deliberate tradeoff for shipping speed; it adds a slight risk of throwaway/typo'd email addresses but no security risk (passwords are still verified at login).

OAuth (GitHub/Google sign-in) is **not** in v1 but may be added in v1.x without breaking existing accounts (each user gets a `users.password_hash` that's nullable; OAuth users would have null password and a row in a separate `auth_providers` table).

## Consequences

### Positive
- **Open to all users** — no requirement that they have a GitHub/Google account.
- **No third-party auth dependency** — if GitHub OAuth goes down, our app doesn't.
- **Full control over the auth UX** — we can make signup as friction-free as we want.
- **Cleanest data model** — every user is a row in `users`; no auth-provider tables in v1.
- **Educational value** — implementing real auth teaches Vishal bcrypt, JWT, session cookies, rate-limiting, password-reset flows.

### Negative
- **More code to write** — signup, login, logout, forgot-password, reset-password are all separate flows. OAuth would have been ~3 routes; this is ~6.
- **We store password hashes** — a security liability. Compromise of the DB means hashed passwords leak. Mitigated by bcrypt cost factor 12 (slow brute-force) and never logging passwords.
- **Email infrastructure required** — to send password resets, we need an email service (see [ADR-0003](0003-email-provider-resend.md)).
- **Rate-limiting must be enforced** — without it, brute-force attacks are trivial. We need either an in-DB rate counter or a Redis-like store.
- **No "social proof" sign-up** — users can't see "100k people use Sign in with Google" on our app.
- **Onboarding friction is higher** — typing email + password is slower than OAuth's one-click.

## Alternatives considered

### Alternative 1 — GitHub OAuth only
**Rejected** because:
- Restricts user base to GitHub users (~95% of non-developer prospects don't have a GitHub account).
- Conflicts with Vishal's "real production app open to anyone" goal.
- Would still need email for password resets if we later added email auth.

### Alternative 2 — Magic link (passwordless email)
**Rejected** because:
- Adds friction *every* login (user must check inbox to sign in repeatedly).
- Email deliverability issues become user-facing login failures — fragile UX.
- Requires email infra anyway, so no infrastructure savings.
- Could be added as an *option* later alongside email+password.

### Alternative 3 — Auth provider (Clerk, Auth0, Supabase Auth)
**Rejected** because:
- "Black box" — Vishal wants to learn how auth actually works, not consume a SaaS.
- Vendor lock-in; migration off these providers is painful.
- Free tiers exist but have user-count caps (e.g., Clerk: 10k MAU free) — fine forever for personal use but couples us to a vendor's pricing.
- Doesn't teach the fundamentals (bcrypt, JWT, sessions) that are valuable to know.

### Alternative 4 — Email + password *and* OAuth on day one
**Rejected** because:
- Doubles the auth code surface to test for v1.
- Adds an account-linking edge case ("what if I signed up with email but now try to sign in with Google using the same email?") that's notoriously bug-prone.
- Better to ship email+password first, add OAuth as a clean v1.x feature with the linking story designed deliberately.

## Implementation notes

- Use a vetted bcrypt library (`bcrypt` for Node).
- Reset tokens: 32+ bytes of cryptographic randomness, hashed before storage (don't store the plaintext token even server-side).
- All auth-related endpoints get the rate-limiter middleware.
- All errors on login return the *same* generic message ("Invalid email or password") to prevent user enumeration.
- Password requirements in v1: min 8 chars. No complexity rules (research shows length matters more than required-special-character). Block top-100 most common passwords.

## References

- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- NIST Digital Identity Guidelines (SP 800-63B)
