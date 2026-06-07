# ADR-0003 — Use Resend for transactional email

**Status:** Accepted
**Date:** 2026-06-07
**Decision makers:** Vishal Bhapkar
**Relates to:** [PRD §6.1 F-AUTH-3](../prd.md#61-auth) (password reset), v1.1 reminders feature

---

## Context

Two features in our scope require sending email:

1. **v1, password reset** ([ADR-0001](0001-auth-email-password.md)) — when a user requests a password reset, we email them a single-use link. Without this, users locked out of their account have no recovery path.
2. **v1.1, daily/scheduled reminders** — surfacing captures whose `remind_at` has arrived.

Constraints:
- **Hard requirement: free tier.** Vishal explicitly does not want a credit card on file or surprise charges.
- Modern developer experience (a simple SDK or HTTP API; we don't want SMTP plumbing).
- Reliable deliverability (Gmail/Outlook should accept our mails without going to spam).
- Reasonable free-tier quotas — at least 100 emails/day, enough to cover sporadic password resets plus a few reminders per user.

## Decision

We will use **Resend** as our transactional email provider.

- **Free tier:** 3,000 emails/month, 100 emails/day, free forever
- **Sender domain:** initially the Resend-provided sender (`onboarding@resend.dev`-style) for development; production will require setting up our own domain with DNS records (SPF/DKIM) for proper deliverability. This is documented in the deployment runbook.
- **Integration:** Resend's official Node SDK, called from server-side code only (API key never exposed to browser).
- **Templates:** maintained as TSX components co-located with the code that triggers them, using `@react-email/components` for type-safe email markup.

## Consequences

### Positive
- **Free forever at our usage scale** — even with hundreds of users, we'd send ~30 emails/day (password resets are rare; reminders are a few per active user/day).
- **Best-in-class developer experience** — Resend is known for clean API and great Next.js integration.
- **React-based email templates** — same component model as our app UI, less context switching.
- **Modern company with good infra** — Resend handles SPF/DKIM/DMARC, IP reputation, bounce handling.
- **Easy to migrate off later** — the abstraction surface is small (`send({ to, subject, react })`) so swapping providers is straightforward if needed.

### Negative
- **New vendor dependency** — if Resend has an outage, our password resets and reminders stop working. Mitigated by failure being non-catastrophic (users can wait; we log all failed sends).
- **Free tier could fill up at scale** — 100/day is generous but not infinite. If we ever cross 50 active users with reminders, we need to monitor. (Resend's next paid tier is ~$20/month for 50k emails — affordable.)
- **Custom domain required for production deliverability** — sending from `resend.dev` works for dev but may end up in spam in production. Setting up the production domain is a one-time DNS task documented in the runbook.
- **Vendor lock-in is non-zero** — we'd need to swap if Resend ever raised prices unreasonably. Mitigated by the small API surface we use (just `send`).

## Alternatives considered

### Alternative A — SendGrid
**Rejected** because:
- Free tier: 100 emails/day — same as Resend, smaller monthly cap.
- Developer experience considered worse (older API, less polished SDKs).
- Owned by Twilio; lower confidence in long-term product investment.

### Alternative B — AWS SES (Simple Email Service)
**Rejected for v1** because:
- Cheapest at scale (~$0.10 per 1,000 emails) but **requires AWS account setup, IAM, and domain verification before the first email goes out**. High friction for a project where Vishal hasn't used AWS before.
- Free tier (62,000/month) is only free *from EC2 instances*; for non-EC2 senders, it's paid from day one (still cheap, but credit card required).
- Reputation management is on us (warming up an IP, handling bounces). Resend handles this.
- Could revisit at scale (>10k emails/month).

### Alternative C — Brevo (formerly Sendinblue)
**Rejected** because:
- Free tier: 300/day — more generous than Resend daily cap.
- But: less polished SDK, more marketing-focused product positioning, weaker fit for purely transactional use.
- Resend's React-based templates are a better DX win than Brevo's higher daily limit (we won't hit either limit).

### Alternative D — Postmark
**Rejected** because:
- Excellent deliverability and DX — would be the right choice for paid transactional email at scale.
- **No free tier** (only a 100-email trial). Disqualifies it given our hard free-tier constraint.

### Alternative E — Mailgun
**Rejected** because:
- Free tier removed in 2023. Now starts at $15/month minimum.
- Disqualifies on free-tier requirement.

### Alternative F — Self-host SMTP (e.g. via a VPS)
**Rejected** because:
- Email deliverability is genuinely hard — Gmail/Outlook aggressively spam-filter mail from unknown IPs.
- IP warming, SPF/DKIM/DMARC, reverse DNS, bounce handling — substantial operations work.
- Not worth it for a personal-scale project.

## Implementation notes

- The email-sending interface should be thin: `await sendEmail({ to, subject, react })` returns `{ id }` on success or throws on failure.
- All email sends are logged with: timestamp, recipient (hashed for privacy), template name, success/failure, latency.
- Failed sends do **not** retry automatically in v1 (avoid double-sends). Failures are surfaced in logs for manual investigation.
- For password resets specifically: if email send fails, the user-facing flow should still return success (don't reveal whether the email exists in our system — prevents user enumeration). We log the failure server-side.
- API key stored in environment variable `RESEND_API_KEY`, set in Vercel project settings, never committed.

## Monitoring

- Track monthly send volume against the 3,000 cap.
- Alert (via Sentry or a simple cron-emailed report) if we cross 80% of monthly cap.
- Track bounce/complaint rates — spike indicates deliverability issues.
