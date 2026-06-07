# PRD Review Notes — 2026-06-07

**Attendees:** Vishal Bhapkar, Claude
**Outcome:** PRD bumped from v0.1 → v1.0 (locked)

These are not formal decisions (those live in ADRs). These are the discussion threads, gut reactions, and reasoning that didn't make it into the PRD itself but are worth preserving.

---

## Pain-point discovery process

Before drafting the PRD, we ran a structured self-interview (8 questions) to uncover Vishal's actual day-to-day pain points rather than picking an idea off a generic list. The technique: surface frictions through questions like "what's the last thing you forgot that mattered?" and "what's the most-edited note in your phone?"

The signal that emerged was unambiguous: **daily exercise + daily study + capturing forgettable tasks** was the cluster of pain points, and Vishal explicitly stated exercise daily was "most important right now."

Secondary pain points identified but **deferred** to future versions:
- Saved-content sprawl (links sent to self via WhatsApp, YouTube playlists forgotten)
- Automatic expense tracking from phone payments
- Passwords stored in Google Keep (resolvable separately with a password manager)

The choice to focus the MVP on the *primary* pain point — and resist adding the secondary ones — is the most important early decision. PRD non-goals section (§4) captures this explicitly so future temptation to scope-creep is constrained.

---

## Discussion on auth method (OQ-1)

**Initial recommendation:** GitHub OAuth (no password storage, no email infra, modern UX).

**Vishal's pushback:** "There may be people without using github meaning other users than me. Also if user forgets password give him forgot password option so a verification is sent to him for creating new password."

**Resolution:** Email + password. The reasoning here is *product positioning*, not technical preference — Vishal sees this as a real app open to anyone, not a developer tool. The point about non-developer users is correct and decisive.

This is captured formally in [ADR-0001](../decisions/0001-auth-email-password.md). The ADR also documents that OAuth can be added in v1.x without breaking the email+password schema (nullable `password_hash`, optional `auth_providers` table).

The implication — that we now *also* need transactional email infrastructure for password resets — surfaced [ADR-0003](../decisions/0003-email-provider-resend.md).

---

## Discussion on streak grace (OQ-4)

**Vishal's initial response:** "I did not understand about this streak importance. Elaborate more its usage."

This was a productive moment. The streak mechanic is so core to the product's motivation model that it warrants explicit shared understanding. We covered:
- Why streaks work (Duolingo data: streak length predicts daily app open)
- The asymmetric risk (a broken streak frequently triggers user dropout — the motivator becomes a quit trigger)
- Four design options with tradeoffs

**Vishal's pick:** Option B — silent grace (one missed day per 7-day window forgiven, invisibly).

This is captured in [ADR-0002](../decisions/0002-streak-freeze-silent-grace.md) with formal rules and edge cases.

---

## Non-goals — Vishal accepted as-stated

Vishal did not push back on any non-goals (§4 of PRD). Specifically he accepted:
- No detailed workout logging in v1 (deferred to v2 module)
- No detailed study tracking in v1 (deferred to v2 module)
- No auto-detection of activity from phone sensors
- No gamification

He also stated: "for other 2 questions lets go with what we have now and then we can build up in next versions."

This is a healthy MVP discipline. The temptation to add the workout module — given that "exercise daily" is his stated #1 goal — was real. We chose to keep the MVP focused on the *habit* layer first (the meta-skill: doing it consistently), with the deep workout-logging module as a v2 candidate informed by usage.

---

## Documentation philosophy (this meta-decision)

Vishal explicitly requested: "Lets keep record of everything we are doing like PRD, tradeoffs etc and future docs and finalyse solution so along with code we can also showcase this I mean it should look like real production app basically."

This is the rationale for setting up `docs/` with: PRD, decisions/ (ADRs), discussions/ (notes like this one). The goal is that the documentation be a first-class artifact alongside the code — the kind of thing a senior engineer joining a well-run team would expect to find.

The structure we adopted is modeled on common practice at well-run modern startups (early-stage Stripe, Linear, etc.):
- One canonical PRD per product area, versioned, never silently changed
- ADRs for every significant decision
- Discussion notes for the threads that don't quite merit an ADR but are worth preserving

---

## Outstanding for next session

1. Pick a real name for the app (not "Daily"). Options to consider on next session.
2. Write the architecture document — system diagram, tech stack ADR, deployment topology.
3. Design the database schema (will involve more ADRs: ORM choice, migration strategy).
4. Set up the repo with linting/formatting/CI before writing any feature code.

---

## Time spent

PRD draft + review + locking: ~1 session (roughly 1 hour).
Documents produced: PRD (v0.1 → v1.0), 3 ADRs, this discussion log, docs README, decisions README.
