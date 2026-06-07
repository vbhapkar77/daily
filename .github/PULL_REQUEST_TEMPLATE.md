<!--
Thanks for opening a PR! Please fill in the sections below so reviewers can
understand the change quickly.

PR title MUST follow Conventional Commits format:
  feat(scope): description       — new feature
  fix(scope): description        — bug fix
  docs(scope): description       — docs only
  refactor(scope): description   — code change that's neither feature nor bug
  test(scope): description       — tests added/changed
  chore(scope): description      — tooling, deps, build
  ci(scope): description         — CI/workflow change
  spec(scope): description       — spec or ADR change
-->

## Summary

<!-- One or two sentences. What does this PR do and why? -->

## Changes

<!-- Bulleted list of the meaningful changes. Reviewers should be able to scan this
     and decide whether to read the full diff or skim it. -->

- ...

## Refs

<!-- Link to the relevant spec/ADR/issue. -->

- Spec: `specs/NNN-feature-name/spec.md` (AC-X, AC-Y)
- Task: `specs/NNN-feature-name/tasks.md` (T-NNN)
- ADR: `docs/decisions/NNNN-decision.md` (if relevant)

## Test plan

<!-- How did you verify this works? Reviewers should be able to follow these steps. -->

- [ ] Unit tests added/updated and passing locally (`./mvnw -B verify` or `pnpm test`)
- [ ] Integration tests added/updated (if touching DB or HTTP layer)
- [ ] Manual smoke test described:
  1. ...
- [ ] CI green on this PR

## Screenshots / output

<!-- If UI, attach before/after screenshots. If CLI, paste relevant output. Otherwise delete this section. -->

## Checklist

- [ ] PR title follows Conventional Commits format
- [ ] Branch name follows `feat/...`, `fix/...`, `chore/...`, or `docs/...`
- [ ] Documentation updated (PRD / ADR / learnings / inline comments as relevant)
- [ ] No secrets or credentials in the diff
- [ ] If touching auth/security, the change has been reviewed against [`AGENTS.md`](../AGENTS.md) §"Conventions — do/don't"
