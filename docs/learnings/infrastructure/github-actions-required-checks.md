# GitHub Actions: required checks vs. paths-filtered workflows — the unmergeable PR trap

> A classic operational pitfall when you combine "required status checks" (branch protection)
> with "paths-filter workflows" (the `paths:` key on `on:`). Easy to introduce, surprisingly
> hard to debug, comes up in every "tell me about a CI/CD problem you solved" interview.

---

## Why this exists / the problem it solves

GitHub gives you two independent features that everyone uses together:

1. **Paths-filtered workflows.** A workflow that only runs when files matching a pattern change:
   ```yaml
   on:
     pull_request:
       paths:
         - 'backend/**'
   ```
   The point: don't waste CI minutes running backend tests on docs-only changes.

2. **Required status checks.** A branch protection rule that says: "PRs targeting `main` must have these named checks reporting success before they can merge."

Each is sensible. Combined naively, they create an unmergeable-PR trap:

> **If a check is "required" but its workflow is "paths-filtered," a PR that doesn't match the paths will leave the check in "Expected — Waiting for status to be reported" forever. The PR can never merge.**

We hit this immediately after enabling branch protection on Daily (PR #2 was docs-only; backend's required `build / test` and docs' required `openapi spectral lint` never started). Took ~5 minutes of confusion to spot. It's the kind of bug that's obvious in retrospect but stops you cold the first time.

---

## How we hit it in Daily

Setup at the time of PR #2:

```yaml
# .github/workflows/backend.yml
on:
  pull_request:
    paths:
      - 'backend/**'
      - '.github/workflows/backend.yml'
```

Branch protection on `main`:
- Required check: `build / test` (the backend workflow's job)
- Required check: `openapi spectral lint` (the docs workflow's job)

PR #2 ("production hygiene") changed only files in `.github/`, root markdown files (`SECURITY.md`, `CHANGELOG.md`, etc.), and `README.md`. **No** files under `backend/` or `docs/api/`.

Result on the PR page:

```
2 pending checks
○ build / test               Expected — Waiting for status to be reported    Required
○ openapi spectral lint      Expected — Waiting for status to be reported    Required
```

These weren't "running and slow" — they were "registered as required but never triggered." The "Squash and merge" button was disabled with no path forward.

Our fix in [PR #2 itself](https://github.com/vbhapkar77/daily/pull/2): removed the `paths:` filter from `backend.yml`, `frontend.yml`, and `docs.yml`. They now run on every PR. Modifying the workflow file in the PR also caused them to trigger that same PR (workflow file changes always run the workflow), unblocking the merge.

---

## The concept (in your own words)

### The three solution patterns

**Pattern A — Always-run (what we did)**

Remove the `paths:` filter. Workflows run on every PR. Simple, correct, slight CI cost.

- ✅ Trivial to implement. No new infrastructure.
- ✅ Works with any branch protection config.
- ❌ Wastes CI minutes on irrelevant runs (e.g., backend test on a docs-only PR).
- 🎯 **Best for:** small teams, public repos with free CI, projects where total CI time is already short.

**Pattern B — Aggregator workflow**

Keep `paths:` filters on the real workflows. Add a single always-running "aggregator" workflow that waits for the others and reports a synthesized check. Make only the aggregator required.

```yaml
# .github/workflows/required.yml
name: required
on:
  pull_request:
    branches: [main]
jobs:
  done:
    runs-on: ubuntu-latest
    needs: []  # we explicitly want NO dependencies
    steps:
      - name: All path-filtered checks completed (success or skipped)
        run: echo "Aggregator check — this is the single 'required' status."
```

In branch protection, you only require `required / done`. The actual workflows still run on relevant changes and you can see their results in the PR's checks list, but they don't block.

- ✅ Keeps the cost savings of paths-filter.
- ✅ One required check is simpler to manage in branch protection.
- ❌ Aggregator doesn't actually verify the sub-workflows ran successfully — it just always passes. Risk: silent skipping.
- 🎯 **Best for:** teams that genuinely need to save CI minutes; willing to accept the trust-it-skipped-correctly tradeoff.

**Pattern C — Conditional with `dorny/paths-filter`**

Workflow runs on every PR but uses an action to detect what changed and conditionally execute work:

```yaml
on:
  pull_request:
    branches: [main]

jobs:
  changes:
    runs-on: ubuntu-latest
    outputs:
      backend: ${{ steps.filter.outputs.backend }}
    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v3
        id: filter
        with:
          filters: |
            backend:
              - 'backend/**'
              - '.github/workflows/backend.yml'

  build:
    needs: changes
    runs-on: ubuntu-latest
    steps:
      - name: Real work (only when relevant files changed)
        if: needs.changes.outputs.backend == 'true'
        run: ./mvnw -B verify
      - name: Skip work, report success (when irrelevant)
        if: needs.changes.outputs.backend != 'true'
        run: echo "No backend changes — required check reports success."
```

- ✅ Best of both: the required check name reports, and you only do expensive work when needed.
- ❌ More complex YAML; new contributors need to understand the `if:` flow.
- 🎯 **Best for:** larger projects where CI minutes genuinely matter (paid runners, monorepos, very fast iteration cycles).

### Which pattern wins?

| Project shape | Pattern |
|---|---|
| Public repo, small team, short CI | **A — always run** (what Daily uses) |
| Same but cost-sensitive | **B — aggregator** |
| Large monorepo with paid CI minutes | **C — conditional dorny/paths-filter** |
| Heavy customization needed (e.g., per-platform matrix) | **B or C** with reusable workflows |

---

## Common interview framing

> **Q: "Tell me about a CI/CD issue you debugged."**
> A: "Right after I enabled branch protection on a repo, my next PR went unmergeable. Two required checks were stuck on 'Expected — Waiting for status to be reported.' Turned out my workflows had `paths:` filters and the PR didn't touch those paths, so the required workflows never triggered. The fix was to either remove the paths filter, or switch to an aggregator-workflow pattern that always reports a synthetic check. The deeper lesson: `paths:` filters and `required status checks` are independent features that don't reason about each other — combining them naively creates this deadlock. I document this gotcha for anyone introducing branch protection to a new repo."

> **Q: "What's the tradeoff between always-running and paths-filtered CI?"**
> A: "Paths filters save CI minutes by skipping work unrelated to the change. They work great with simple `on: push:` flows. But they don't compose well with branch protection's required status checks — a PR that doesn't match the filter has no check to satisfy the requirement, so it can't merge. Three solutions: (a) always run, accept the cost; (b) use an aggregator workflow as the single required check while keeping paths-filter on the real workflows; (c) always run the workflow but conditionally execute work using something like `dorny/paths-filter`. Choice depends on your CI cost and tolerance for complexity."

> **Q: "What is `dorny/paths-filter` and when would you use it?"**
> A: "It's a GitHub Action that compares the changed files in a PR against patterns you define and outputs which patterns matched. You'd use it inside an always-running workflow to conditionally execute steps — for example, run backend tests only if `backend/**` changed, but still report the workflow as successful (so it satisfies the required-check rule) when it didn't. Less common than `paths:` filter for simple cases but essential for the 'always-runs-but-skips-work' pattern that plays nicely with branch protection."

> **Q: "How would you debug a PR stuck on 'Expected — Waiting for status to be reported'?"**
> A: "First, check the workflow's `on:` config — likely a `paths:` filter that doesn't match the PR's files. Second, check branch protection settings to see what checks are required and confirm they map to actual workflow job names. Third, push an empty whitespace change to a file that does match the paths filter; if that triggers and unblocks, the diagnosis is confirmed. The proper fix is one of the three patterns from the previous answer, not a band-aid push."

---

## Gotchas & misconceptions

- **The check name in branch protection must exactly match the workflow's job name** (or `name:` inside the job), NOT the workflow file name. Easy to mis-configure.
- **Workflow file changes always trigger the workflow,** even with `paths:` filter. This is sometimes a useful escape hatch (modify the workflow to force a trigger) but mostly a footgun (changing a workflow runs it on the same PR — confusing if the change is breaking).
- **`paths-ignore` is the inverse**: "skip the workflow when only these files changed." Same trap applies if you combine it with required checks.
- **Pattern B's aggregator check can be lied to.** If you write `if: always()` carelessly, the aggregator says "pass" even when sub-workflows failed. Be explicit about what success means.
- **Required checks block merge but don't run the workflow themselves.** Branch protection is a *reader* of check statuses, not a workflow trigger. The workflow has to be triggered separately by the `on:` config.
- **GitHub doesn't warn you about this combo at setup time.** You set up branch protection in the UI; you set up workflows in YAML. They don't cross-validate. You discover the issue with your next PR.

---

## Related concepts

- See [[infrastructure/branch-protection]] (coming later) for the full set of branch protection rules: linear history, signed commits, required reviews.
- See [[infrastructure/conventional-commits]] (coming later) for the related "enforce PR title format" pattern.
- See [[testing/coverage-as-required-check]] (coming later) for using JaCoCo coverage as a required threshold.

---

## Further reading

- [GitHub docs — Triggering a workflow with paths](https://docs.github.com/en/actions/using-workflows/triggering-a-workflow#using-filters) — the official explanation of `paths:` and `paths-ignore:`
- [GitHub docs — About protected branches](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches) — the required-check semantics
- [GitHub Community discussion of this exact bug](https://github.com/orgs/community/discussions/13690) — long thread with the most popular workarounds
- [dorny/paths-filter](https://github.com/dorny/paths-filter) — the canonical action for conditional execution
- [Mark Vaughn — "GitHub Actions: Required Checks for Path-Based PRs"](https://markphelps.me/posts/2023/) — blog walking through Pattern C in detail

---

*Note created: 2026-06-08 after hitting this in PR #2 (production-hygiene). Resolved with Pattern A.*
