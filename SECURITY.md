# Security Policy

We take security seriously. If you discover a vulnerability in Daily, please report it
responsibly so we can fix it before it can be exploited.

## Reporting a vulnerability

**Please do NOT report security vulnerabilities through public GitHub issues, discussions, or pull requests.**

Use GitHub's **Private Security Advisory** feature:

➡ [**Report a vulnerability privately**](https://github.com/vbhapkar77/daily/security/advisories/new)

Alternatively, email **vbhapkar77@gmail.com** with the subject `[SECURITY] Daily —` followed by a brief title.

## What to include

When you report, please include as much of the following as you can:

- A clear description of the vulnerability and its impact
- Steps to reproduce it (or a working proof-of-concept)
- The affected version / commit / URL
- Your assessment of severity
- Any mitigations you can think of

## What to expect

- **Acknowledgment** within 3 business days of your report
- A first-pass triage decision within 7 business days
- Fix released as quickly as practical, with the severity dictating urgency:
  - **Critical** (RCE, data leak, auth bypass): patched within 7 days
  - **High** (privilege escalation, persistent XSS): patched within 14 days
  - **Medium / Low**: patched in the next regular release

Once the fix is published, we'll publicly credit you (with permission) in the release notes
and security advisory.

## Supported versions

This project is currently in early development (pre-1.0). Only the `main` branch is supported;
older commits will not receive security backports.

## Scope

In-scope for security reports:

- Anything in this repository (`vbhapkar77/daily`)
- The deployed production app at:
  - https://daily-seven-olive.vercel.app
  - https://daily-api-nteb.onrender.com

Out of scope:

- Vulnerabilities in third-party services we use (Vercel, Render, Neon, Resend) — report directly to them
- Denial-of-service via excessive request volume (we rely on the platforms' rate limiting)
- Social engineering / phishing
- Issues requiring physical access to the user's device

## What's a vulnerability vs. a bug?

Vulnerabilities exploit a security property: authentication can be bypassed, an unauthorized
user can read another user's data, a request can corrupt server state, etc. Functional bugs
("the button doesn't work") should use the regular [bug report issue template](https://github.com/vbhapkar77/daily/issues/new/choose).

When in doubt, err on the side of private disclosure.

## Hall of fame

We'll list reporters who've helped here once we have any:

*(empty)*
