# ADR-0002 — Streak survives one missed day per 7-day window (silent grace)

**Status:** Accepted
**Date:** 2026-06-07
**Decision makers:** Vishal Bhapkar
**Relates to:** [PRD §6.4 F-STR-1a](../prd.md#64-streaks--history)

---

## Context

Streaks are the most powerful motivational mechanic in habit-tracking apps. Duolingo's published data shows that streak length is the single largest predictor of daily app opens. However, the streak design carries an asymmetric risk:

- A streak motivates the user *while it's alive*
- A reset-to-zero streak often **causes user dropout** — research and post-mortems from habit apps consistently show that users who break a long streak frequently quit the app entirely. The mechanic intended to motivate becomes the trigger for quitting.

We need to decide what happens when a user misses a day.

Real-world context: humans get sick, travel, have unexpected emergencies, or simply forget once. A system that punishes one off-day equally to chronic non-adherence does not match how *actually-consistent* people operate (no athlete trains 365/365 — they train 6/7 weeks indefinitely).

## Decision

A habit's streak survives **one missed day per rolling 7-day window**.

### Formal rule

Let `today` be the user's local date. For each habit:

1. Walk backwards from `today` (or `yesterday` if `today` is not yet checked) day by day.
2. Count consecutive "done" days. A day counts as part of the streak if either:
   a. it was marked done, OR
   b. it was missed AND no other missed day exists within the 7 days *prior to it* (inclusive of the day itself going back 7 days).
3. Stop at the first day that doesn't qualify. That's the streak length.

### Examples

| Pattern (today first, ✓=done ✗=missed) | Current streak | Why |
|---|---|---|
| `✓ ✓ ✓ ✓ ✓ ✓ ✓` | 7 | Perfect week. |
| `✓ ✓ ✓ ✗ ✓ ✓ ✓` | 7 | One miss, surrounded by done days. Forgiven. |
| `✓ ✓ ✓ ✗ ✗ ✓ ✓` | 0 | Two misses in 7 days. Streak broken. |
| `✓ ✓ ✓ ✗ ✓ ✓ ✓ ✓ ✓ ✓ ✓ ✗ ✓` | 13 | Two misses, but they're >7 days apart. Both forgiven. |
| `✗ ✓ ✓ ✓ ✓ ✓ ✓` | 6 if today not yet checked-in. Streak counts back from yesterday. |

### UI rules

- The streak number displayed (`🔥 14`) is the *effective* streak after applying the grace rule.
- The heatmap is **honest** — missed days are visibly missed (lighter color), even when the streak survives. The grace is in the *counter*, not in the data.
- We **never** show the user "you have 1 grace day remaining this week" or any equivalent. The grace is silent; it should feel like the streak is forgiving rather than gamified.
- "Longest ever" streak uses the same calculation logic for consistency.

## Consequences

### Positive
- **Higher retention** — one bad day doesn't trigger user quit.
- **Matches reality** — consistent humans skip days occasionally.
- **No new UI surface area** — the user just sees a streak number, same as a simple app.
- **Honest heatmap** — users can see their actual pattern; the grace doesn't hide truth.

### Negative
- **Slight "magic" feel** — a user who notices "wait, I missed yesterday but my streak is still 14?" may briefly be confused. We accept this. (Future: optional explainer in settings or hover tooltip.)
- **Streak calculation is more complex** — not just `count consecutive done days backward`. Edge cases require tested code. Mitigated by comprehensive unit tests in `lib/streak.ts`.
- **Possible to game** — a user could intentionally skip every Wednesday forever and keep an infinite streak. We accept this because (a) the user is the only person they're "fooling" and (b) it's still better than the dropout from hard reset.
- **Heatmap and streak number can disagree** at a glance — the user sees a missed day in the heatmap but a streak that "should" be broken. Mitigated by clear visual treatment (missed-but-forgiven days could get a slightly different color in v1.1 if confusion emerges).

## Alternatives considered

### Alternative A — Hard reset (one miss = streak 0)
**Rejected** because:
- Documented to cause user dropout in habit apps.
- Mismatched with how consistent humans actually behave.
- "Motivator becomes quit trigger" failure mode.

### Alternative C — Explicit "rest day" UI
User marks "rest day" in advance; it counts as not breaking streak.

**Rejected for v1** because:
- Adds a UI affordance (button, modal, state) when zero UI is the design goal.
- Forces the user to *plan* their inconsistency, which is unrealistic.
- Could be added as an optional power-user feature later.

### Alternative D — Two numbers: "current" (hard reset) + "longest ever" (persists)
**Rejected** because:
- Doesn't actually solve the dropout problem — users still see their current streak hit 0 and feel ruined.
- "Longest ever" is a vanity metric that may demotivate as it grows ("I'll never beat my 87-day streak").

### Alternative E — Duolingo-style streak freezes (earned/purchased tokens)
**Rejected** because:
- Introduces gamification we explicitly named as a non-goal (PRD NG-4).
- Requires monetization or earning mechanics we don't want.
- Patronizes the user.

## Implementation notes

- The streak calculation should be in a pure function with no I/O: `calculateStreak(checkIns: CheckIn[], today: Date, timezone: string): { current: number, longest: number }`.
- All edge cases (timezone boundaries, DST, today-not-yet-checked) must be unit-tested. Target: 100% line coverage on the streak module.
- Cache the streak number on the habit row for read performance, but always recompute on write. Never trust the cache during writes.
- The 7-day window for the grace rule uses *user local days*, not 168-hour windows.

## Open questions for v1.1+

- Should the user be able to see their "skipped but forgiven" days highlighted differently in the heatmap?
- Should we expose `longest_ever` streak in the UI from v1, or wait?
- Should we allow more than 1 grace per 7 days for habits with low frequency targets (e.g., a 3x/week goal)?
