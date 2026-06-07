# Product Requirements Document

| | |
|---|---|
| **Working name** | Daily |
| **Version** | v1.0 (locked) |
| **Status** | 🟢 Locked — implementation in progress |
| **Owner** | Vishal Bhapkar |
| **Author** | Vishal + Claude (pair-drafted) |
| **Last updated** | 2026-06-07 |
| **Audience** | Single-builder team (Vishal). Document is also written so a future engineer joining cold can ramp up in 15 minutes. |

---

## 1. TL;DR

A personal daily check-in app for people who set themselves daily aspirations (exercise, study, etc.) but lack a system to actually follow through. One screen, opened once a day, mark off what you did, capture anything you don't want to forget, see your streaks and patterns. Built mobile-first, opinionated, fast, no clutter.

---

## 2. Problem

**The problem we're solving:**

Self-motivated people set daily intentions — *I want to exercise daily, study daily, stop forgetting important things* — but the gap between intention and follow-through is enormous. The existing tools fail in specific ways:

- **Generic todo apps** (Todoist, TickTick) treat habits as recurring tasks. Recurring tasks feel like chores; missing one is silent. You can't see your *streak* or *pattern* at a glance — the motivational feedback loop is missing.
- **Gamified habit apps** (Habitica, HabitNow) add cartoon RPG mechanics that turn the experience into a video game. Adults who are serious about their goals feel patronized.
- **Pure habit trackers** (Streaks, Done, Way of Life) are austere and *only* track habit ticks — no place to capture the "I need to buy running shoes" thought that just popped into your head while looking at your fitness streak. So users keep a habit tracker AND a notes app AND a reminders app, and things fall through the cracks between them.
- **Notes apps used as workarounds** (Apple Notes, Google Keep) are unstructured. There's no streak feedback. No reminders that surface things back to you. The user is doing the cognitive work of remembering to look at the note.

The pain point this app addresses is **the gap between "I want to be consistent about a few important things every day" and "actually doing them" — combined with the cognitive load of "things I need to remember but have nowhere good to put."**

---

## 3. Target User

### Primary persona — "The Aspiring Builder"

**Profile:** A 25–40 year old knowledge worker (often a software engineer, designer, PM, or similar). Self-motivated. Reads self-improvement content. Already tried 3+ habit tracking apps and dropped all of them. Has a few clear goals (usually: get fit, learn a skill, build consistency) and is frustrated with the gap between intention and execution. Comfortable with technology — they have a smartphone they always carry and a laptop. Privacy-conscious; doesn't love giving health data to big platforms.

**Quote from initial user research (Vishal, 7 June 2026):**

> "Exercise daily, this is most important right now. I want to build a habit for exercises to become fit from skinny fat. I also study new topics each day. And I forgot many tasks like purchasing something or researching something which is important for my life."

### Secondary persona — "The Quiet Achiever" (future)

A more privacy-focused, less tech-forward user (parents, family members) who wants the same daily check-in benefit but with simpler UI and gentler push.

*(MVP optimizes for the primary persona. Secondary persona informs v2+ work.)*

---

## 4. Goals & Non-Goals

### Goals (v1)

| # | Goal | Measurable as |
|---|---|---|
| G1 | Get the user to check in daily for 30 consecutive days | Streak length on primary habit after 30 days |
| G2 | Reduce the user's mental load of remembering personal tasks | Number of captures used / week |
| G3 | Make the daily check-in feel *fast* — open, mark, close | Time from app open to check-in submitted < 5 seconds |
| G4 | Make missing a day *visible* without making it punishing | Heatmap is the dominant visual; no notifications scolding |
| G5 | Work equally well on phone (primary) and laptop | Same UI both, no separate mobile app |

### Non-goals (explicitly NOT what this is)

| # | Non-goal | Why we're not doing it |
|---|---|---|
| NG1 | Full task / project manager | Todoist and friends already solve this well. Captures here are lightweight "don't forget" notes, not full task management. |
| NG2 | Detailed workout logging (sets, reps, weight, exercises) | Strong, Hevy, and Hevy exist and do this better. Out of scope for v1. *Could become a v2 module.* |
| NG3 | Detailed study tracking (flashcards, spaced repetition, curriculum view) | Anki exists. v1 just logs *that* you studied + a free-text note about *what*. |
| NG4 | Gamification (XP, levels, achievements, avatars) | Patronizing for the target user. Streak + heatmap is the only motivator. |
| NG5 | Social / multi-user (friends seeing your streaks) | Adds privacy concerns, dilutes focus. v3+ candidate at earliest. |
| NG6 | Automatic data input (auto-detect exercise from phone sensors, etc.) | High complexity, low MVP value. Manual one-tap check-in is the design intent. |
| NG7 | AI features (auto-summarize study notes, AI habit suggestions) | Resist temptation. v2 candidate after we know what users actually do. |
| NG8 | Custom theming, dark/light toggle, fonts | One tasteful default is fine. No customization. |
| NG9 | Export / API / integrations (Strava, Apple Health, etc.) | v2+ at earliest. |

---

## 5. User Stories (MVP)

In standard *as a / I want / so that* format. Numbered for traceability through the codebase and tests.

| ID | User story |
|---|---|
| US-01 | As a new user, I want to **sign in with my existing account** so that I don't have to remember another password. |
| US-02 | As a new user, I want to **configure a small number of habits** (e.g., "Exercise", "Study") on first sign-in so that the app immediately reflects what I care about. |
| US-03 | As a user opening the app on any device, I want to **see today's check-in** front and center so that I can mark off in seconds. |
| US-04 | As a user, I want to **mark a habit done with one tap** so that the friction is near-zero. |
| US-05 | As a user, I want to optionally **add a short note** to a check-in (e.g., "trained legs", "Spring Boot security") so that I capture *what* I did without it being mandatory. |
| US-06 | As a user, I want to **see my current streak per habit** so that I have a visible reason not to break it. |
| US-07 | As a user, I want to **see a 12-week heatmap per habit** so that I can see my pattern at a glance. |
| US-08 | As a user, I want to **capture a one-line "don't forget" note at any time** so that I can offload my brain without context-switching to a separate app. |
| US-09 | As a user, I want to optionally **set a reminder date on a capture** (today, tomorrow, next week, custom) so that the app surfaces it back to me when relevant. |
| US-10 | As a user, I want to **see captures due today** at the top of the home screen so that I act on them without hunting. |
| US-11 | As a user, I want to **mark a capture done or dismiss it** so that it doesn't keep nagging me. |
| US-12 | As a user, I want to **edit a habit** (rename, delete, reorder) so that my list evolves with me. |
| US-13 | As a user, I want to **back-fill yesterday's check-in** if I forgot to open the app so that one missed evening doesn't reset my streak. (Open for review — see §11) |
| US-14 | As a user, I want my data to follow me across devices and sessions so that I can check in from my phone and review on my laptop. |
| US-15 | As a user, I want to **sign out** so that I can use the app on a shared device safely. |

---

## 6. Functional Requirements (MVP)

Grouped by area. Each requirement is testable.

### 6.1 Auth
- **F-AUTH-1** User can **sign up with email + password** (min 8 chars, validated). Password is bcrypt-hashed before storage. See [ADR-0001](decisions/0001-auth-email-password.md).
- **F-AUTH-2** User can sign in with email + password.
- **F-AUTH-3** User can request a password reset; system emails a single-use, time-limited (1 hour) reset link to the registered address.
- **F-AUTH-4** Login is rate-limited per IP and per email to prevent brute force (max 5 attempts per 15 minutes; lockout 15 min).
- **F-AUTH-5** Session persists for 30 days via HttpOnly secure cookie containing a signed session token.
- **F-AUTH-6** User can sign out, which invalidates the session immediately.
- **F-AUTH-7** Email verification on signup is **deferred to v1.1** (in v1, new accounts are usable immediately).

### 6.2 Habits
- **F-HAB-1** User can create a habit with a name (max 50 chars) and an emoji (optional).
- **F-HAB-2** User can have 1–10 active habits. (Hard cap; UX gets cluttered above 10.)
- **F-HAB-3** User can rename, delete, or reorder habits at any time.
- **F-HAB-4** Deleting a habit *archives* it — its historical check-ins are preserved but the habit no longer appears on the home screen. (No hard delete in v1.)

### 6.3 Daily check-in
- **F-CHK-1** The home screen always shows today's date + a list of all active habits with check/uncheck controls.
- **F-CHK-2** Tapping a habit toggles its done-state for today. Optimistic UI — checkmark appears instantly, syncs to server in background.
- **F-CHK-3** User can add a short note (max 200 chars) to a check-in.
- **F-CHK-4** User can change yesterday's check-in for up to 24 hours after the day ends. (See US-13; deferred for review.)

### 6.4 Streaks & history
- **F-STR-1** Current streak per habit is calculated as the count of consecutive prior days (including today if checked) the habit was marked done, with a **silent weekly grace** rule (see [ADR-0002](decisions/0002-streak-freeze-silent-grace.md)).
- **F-STR-1a** **Silent grace rule:** within any rolling 7-day window ending at "today", **one missed day is silently forgiven** and does not break the streak. A second missed day within the same 7-day window resets the streak to 0. The UI never reveals which days were "forgiven" — the user just sees their unbroken streak.
- **F-STR-2** A 12-week heatmap per habit shows each day's actual state (done / missed / future). The heatmap is honest — it shows misses even when the streak survives via grace.
- **F-STR-3** History view: scroll back through prior days, see what was checked on each.
- **F-STR-4** Per-habit detail screen shows: current streak, longest-ever streak, % completion this month, 12-week heatmap.

### 6.5 Captures (the "don't forget" inbox)
- **F-CAP-1** User can add a capture from the home screen with one tap (textarea, optional date).
- **F-CAP-2** Each capture has: text (max 280 chars), created_at, optional remind_at, status (open/done/dismissed).
- **F-CAP-3** Captures whose `remind_at` is today or earlier appear in a "Due today" section at the top of the home screen.
- **F-CAP-4** User can mark a capture done or dismiss it (both remove it from the list but preserve history).
- **F-CAP-5** Captures without `remind_at` live in an "Inbox" view, browsable but not surfaced on the home screen.

### 6.6 Timezone
- **F-TZ-1** "Today" is defined by the user's local timezone, set on first login from browser and editable in settings.
- **F-TZ-2** Streaks and heatmaps are calculated per the user's timezone (not server time).

---

## 7. Non-Functional Requirements

### 7.1 Performance
- **NFR-PERF-1** Home screen renders interactive content within 1.5s on a mid-range Android phone over 4G. *(Tested via Lighthouse CI.)*
- **NFR-PERF-2** Toggle a habit → optimistic update visible within 50ms.
- **NFR-PERF-3** Server response p95 for any single API call < 400ms (excluding cold start).

### 7.2 Reliability
- **NFR-REL-1** Uptime target 99% (acceptable for personal-use v1; budget = ~7h downtime/month).
- **NFR-REL-2** Data loss is unacceptable. All writes are durable (Postgres-backed) before the UI confirms success.
- **NFR-REL-3** App works as a static read-only view of cached data when offline. Writes queue and sync when back online. *(Stretch goal — may slip to v1.1.)*

### 7.3 Security & privacy
- **NFR-SEC-1** All traffic is HTTPS only.
- **NFR-SEC-2** User data is isolated per-account at the database level (every row has `user_id`, every query filters on it).
- **NFR-SEC-3** Session cookies are `HttpOnly`, `Secure`, `SameSite=Lax`.
- **NFR-SEC-4** No user content is exposed to third parties (no analytics scripts, no ad pixels).
- **NFR-SEC-5** No tracking beyond what's necessary for the app to function. Anonymous error telemetry (Sentry) only.
- **NFR-SEC-6** User can request full data export (JSON) at any time. *(v1 may be manual via DB query.)*
- **NFR-SEC-7** User can delete their account, which hard-deletes all their data within 30 days. *(v1 may be a manual support process.)*

### 7.4 Accessibility
- **NFR-A11Y-1** All interactive elements are keyboard-reachable.
- **NFR-A11Y-2** Color contrast meets WCAG AA (4.5:1 for body text).
- **NFR-A11Y-3** Screen-reader labels on all icon-only buttons.

### 7.5 Code quality
- **NFR-CODE-1** TypeScript strict mode, no `any`.
- **NFR-CODE-2** Lint + format + typecheck pass on every PR (CI-blocking).
- **NFR-CODE-3** Code coverage on business logic (streak calc, timezone math) >= 90%.
- **NFR-CODE-4** Every PR has a description that explains the *why*, not just the *what*.

---

## 8. UX flow (text mocks)

### 8.1 First-time user flow

1. Land on `/` → see marketing hero + "Sign in" button
2. Click sign in → OAuth flow → return to app
3. Onboarding: "What do you want to be consistent about?" → 2 default habits (Exercise, Study) prefilled → user can edit/add → "Done"
4. Land on home screen — empty state with "Mark today's first check-in"

### 8.2 Daily home screen (the screen the user sees 95% of the time)

```
┌──────────────────────────────────────────────────────┐
│ Daily                          Sun · 7 Jun     v.👤  │
├──────────────────────────────────────────────────────┤
│                                                      │
│   ◐ Due today (1)                                    │
│   • Buy running shoes                  [✓ done] [×]  │
│                                                      │
│   ◐ Today's check-in                                 │
│   [✓] Exercise        🔥 12       + Add note         │
│   [✓] Study           🔥 8        "JVM tuning"       │
│   [ ] No phone in bed 🔥 0                           │
│                                                      │
│   ◐ Quick capture                                    │
│   [ Type something...               ] [Remind ▾] [+] │
│                                                      │
│   ◐ This week                                        │
│   Exercise   ▮▮▯▮▮ ▮ ─                               │
│   Study      ▮▮▮▮▮ ▮ ─                               │
│                                                      │
│   [ See heatmap → ]                                  │
└──────────────────────────────────────────────────────┘
```

### 8.3 Heatmap / detail screen (per habit)

12-week heatmap (GitHub-contributions style). Tap a square = see/edit that day's note. Summary stats: current streak, longest streak, % completion this month.

### 8.4 Capture inbox

List of captures grouped by: Overdue / Today / Upcoming / Inbox. Tap any to edit/done/dismiss.

### 8.5 Settings

Habits manager (add/edit/archive/reorder), timezone, account, sign out, data export request.

---

## 9. Out of scope (v1) — captured so we don't lose them

| Idea | Source | Likely target |
|---|---|---|
| Workout logging (sets/reps/weight) | Vishal's question 1 + 7 | v2 module |
| Study log with topics + sources | Vishal's question 7 + 8 | v2 module |
| Saved links / video bookmarks vault | Vishal's question 6 | v2.5 — could be its own module |
| Automatic expense tracking | Vishal's question 5 | v3 at earliest; technically hard |
| Web push notifications | Need for proactive reminders | v1.1 if MVP works |
| Email reminders | Same | v1.1 |
| Mobile app (PWA install banner) | Reach | v1.1 polish |
| AI features (auto-tagging, suggestions) | Modern stack | v2 — only if user behavior justifies |
| Social / accountability features | Future | v3+ |

---

## 10. Success Metrics

### Personal (the most honest test)
- **PM-1** Vishal uses the app on **25 of 30 consecutive days** after launch.
- **PM-2** Vishal does not maintain a parallel system (Google Keep, Notes) for habits/captures within 30 days.

### Engagement (when there are more users)
- **EM-1** D7 retention (% of users who check in on day 7) > 40%.
- **EM-2** D30 retention > 25%.
- **EM-3** Median user has check-ins on 4+ of 7 days in a given week.

### Quality
- **QM-1** p95 home-screen TTI < 1.5s on mid-range mobile.
- **QM-2** Zero Sentry-tracked unhandled errors per week per active user.

---

## 11. Open Questions — RESOLVED for v1.0

| # | Question | Resolution | Reference |
|---|---|---|---|
| OQ-1 | Auth: GitHub OAuth only, or also email? | **Email + password (primary)**, with reset-via-email flow. No OAuth in v1. | [ADR-0001](decisions/0001-auth-email-password.md) |
| OQ-2 | Back-fill check-ins for past days? | **Yesterday only**, within 24 hours of day end. No further back-fill. | This PRD F-CHK-4 |
| OQ-3 | Reminder mechanism in v1? | **Email only** (via Resend). Web push deferred to v1.1. | [ADR-0003](decisions/0003-email-provider-resend.md) |
| OQ-4 | Streak grace mechanism? | **Silent weekly grace** — one missed day per rolling 7-day window is silently forgiven. | [ADR-0002](decisions/0002-streak-freeze-silent-grace.md) |
| OQ-5 | Single timezone setting vs auto-update on travel? | **Single explicit setting**; user changes manually in settings. | This PRD F-TZ-1 |

---

## 12. Risks & Mitigations

| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R-1 | Vishal stops using it within 2 weeks, app dies | Medium | High | Build for *his* exact stated needs, dogfood from day 1, ship MVP fast (<3 weeks) |
| R-2 | Scope creep: temptation to add workout log / study log / expenses | High | High | This PRD's non-goals section is the contract. Defer all v2 items to v2. |
| R-3 | Timezone edge cases corrupt streaks | Medium | Medium | Comprehensive tests in `lib/streak.ts`; manual QA for DST + travel |
| R-4 | iOS web push is unreliable; reminders feature looks broken | High (if attempted in v1) | Medium | Out of MVP entirely; use email |
| R-5 | Free tier sleep on backend if we use Render | Low (using Vercel Functions) | Medium | Architecture decision will favor always-on (Pattern A) |
| R-6 | Test coverage slips when shipping fast | Medium | Medium | Make CI block merges below 90% coverage on `lib/` |

---

## 13. Future roadmap (NOT MVP — for context)

### v1.1 (polish, ~2 weeks after v1)
- Email reminders for due captures
- PWA install prompt + offline support
- "Streak freeze" / vacation mode (OQ-4)

### v2 (modules — pick one based on MVP usage data)
- **Workout module:** structured exercise logging (sets, reps, weight, PR tracking) that plugs into the Exercise habit
- **Study module:** topic + source + summary, optionally backed by Anki-style spaced repetition
- **Saved content vault:** paste link → AI summary + auto-tag, optional weekly digest

### v3 (only if there's a user base beyond Vishal)
- Public profile / accountability sharing with selected friends
- Mobile app wrapper (Capacitor / native)
- API for power users

---

## 14. Glossary

- **Habit** — A recurring daily intention the user wants to track. E.g. "Exercise", "Study", "No phone in bed".
- **Check-in** — A user's per-day record of whether a habit was done, with optional note.
- **Capture** — A free-form one-liner the user wants to remember, optionally scheduled to surface on a future date.
- **Streak** — Consecutive days a habit has been marked done, counting backward from today (or yesterday if today not yet checked).
- **Heatmap** — A grid visualization of the last N weeks, one square per day, intensity = done state.

---

## 15. Sign-off

| Role | Person | Status | Date |
|---|---|---|---|
| Product | Vishal | 🟢 Signed off | 2026-06-07 |
| Engineering | Vishal + Claude | 🟢 Signed off | 2026-06-07 |
| Design | Vishal | 🟢 Signed off (text-based ASCII mocks accepted; visual design TBD in build phase) | 2026-06-07 |

**v1.0 is locked.** Further changes require a new version + change-log entry below.

---

*Document conventions: this PRD lives at `docs/prd.md`. All updates are PRs. Major changes bump the version and add a row to the change log below.*

## Change log

| Version | Date | Author | Change |
|---|---|---|---|
| 0.1 | 2026-06-07 | Vishal + Claude | Initial draft |
| 1.0 | 2026-06-07 | Vishal + Claude | All 5 open questions resolved (auth, back-fill, reminders, streak grace, timezone). Auth changed from OAuth to email+password per OQ-1 review. Streak grace rule (F-STR-1a) added in detail. ADRs 0001–0003 linked. Locked. |
