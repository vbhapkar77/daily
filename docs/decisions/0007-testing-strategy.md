# ADR-0007 — Testing strategy: full pyramid with Testcontainers and Playwright

**Status:** Accepted
**Date:** 2026-06-07
**Decision makers:** Vishal Bhapkar
**Relates to:** [ADR-0004](0004-tech-stack.md), [ADR-0008](0008-docker.md), PRD [NFR-CODE-3](../prd.md#75-code-quality)

---

## Context

The PRD's non-functional requirement NFR-CODE-3 specifies "code coverage on business logic (streak calc, timezone math) >= 90%." Beyond coverage numbers, we need a coherent **testing strategy**: which layers to test, with which tools, what to mock vs. integrate, and how this gates CI.

Vishal's stated goal includes interview readiness. Every senior backend interview asks about testing strategy — "how do you test your code, what do you mock, how do you do integration testing." We need to live the answer, not just read about it.

The principles guiding the choice:

1. **Test the right thing at the right level.** Don't write end-to-end tests for what a unit test could cover.
2. **Prefer real over mocked at integration points.** Mocking Postgres is the wrong abstraction; testing against H2 in-memory is the wrong DB. We use real Postgres in tests via Testcontainers.
3. **Tests are documentation.** A test that reads as `"given X, when Y, then Z"` documents behavior. Bad tests obscure it.
4. **Speed matters.** A test suite that takes 20 minutes will be skipped. Target: <2 min for the full backend suite.
5. **CI is the source of truth.** Local "it works on my machine" doesn't count. The pipeline is the canonical pass/fail.

## Decision

### Backend test layers

| Layer | Tool | Scope | Speed target | Coverage target |
|---|---|---|---|---|
| **Unit** | JUnit 5 + Mockito + AssertJ | Pure logic (streak calc, password hashing, timezone helpers); services with mocked deps | <100ms each | 90%+ on `lib/`, services |
| **Persistence** | `@DataJpaTest` + **Testcontainers** (real Postgres) | Repository methods, JPA mappings, query correctness, FK behavior | <2s each | 100% of custom queries |
| **Web layer** | `@WebMvcTest` + MockMvc | Controller routing, request/response serialization, validation, auth filters (with mocked services) | <100ms each | All HTTP endpoints have at least one test |
| **Full integration** | `@SpringBootTest` + Testcontainers + WebTestClient | Full Spring context, real Postgres, HTTP → controller → service → repository → DB → response | <5s each | One per critical user flow |

### Frontend test layers

| Layer | Tool | Scope | Speed target |
|---|---|---|---|
| **Unit / component** | Vitest + React Testing Library | Pure functions, custom hooks, components in isolation (no API) | <50ms each |
| **API mock layer** | MSW (Mock Service Worker) | Components against fake backend responses (deterministic, fast) | <200ms each |
| **End-to-end** | Playwright | Real browser (Chromium + WebKit), real backend running in Docker, full user flows | <30s each |

### Cross-cutting

- **Lint:** ESLint (frontend), Spotless or Checkstyle (backend) — zero warnings to merge
- **Typecheck:** `tsc --noEmit` (frontend), Java compiler is implicit
- **Coverage:** JaCoCo (backend) + Vitest coverage (frontend), reported as PR comment
- **Mutation testing:** PITest on backend's `lib/` package — bonus, not blocking. Mutation score >70% as aspirational target.
- **Lighthouse CI:** budget enforced (performance >90, accessibility >90)

### What we **don't** test

- **Framework code.** Spring's annotation handling. Next.js routing. We trust the framework.
- **Generated code.** OpenAPI-generated TypeScript clients. They're code-as-output.
- **Pure constant declarations.** `const ROLES = ['user', 'admin']` doesn't need a test.
- **CSS / visual styling.** No screenshot tests in v1. Could add Percy / Chromatic later.

### CI gates

The following are **required to merge a PR**:
1. Lint passes (zero warnings)
2. Typecheck passes
3. All unit tests pass
4. All integration tests pass (Testcontainers + Postgres)
5. Coverage on changed files >= 80%
6. Coverage on `lib/` (business logic) >= 90%

The following run on `main` after merge (not blocking PRs):
1. Full E2E suite (Playwright)
2. Lighthouse CI performance budget
3. Bundle size check

## Consequences

### Positive
- **High confidence in deployments.** Refactors don't break invisible behaviors because the test suite catches them.
- **Tests as living documentation.** A new engineer reads tests to understand expected behavior.
- **Real DB in tests catches real bugs.** Many bugs are SQL bugs (incorrect type coercion, FK violations, transaction edge cases). H2 hides these. Testcontainers exposes them.
- **Interview gold.** Testcontainers, mutation testing, test pyramid — all are advanced topics that come up in senior interviews. Vishal can speak from experience.
- **Forces good design.** Code that's hard to test is usually code that's hard to maintain. Test-first naturally pushes toward dependency injection and pure functions.

### Negative
- **Test infrastructure setup is non-trivial.** Testcontainers needs Docker running. CI runners need Docker. The first integration test takes effort to bootstrap.
- **Tests are real code that must be maintained.** Coverage targets create pressure to write trivial tests just to bump the number. We mitigate by gating on `lib/` (where coverage genuinely correlates with confidence), not arbitrary global numbers.
- **Slow first-run of integration tests.** Pulling Postgres Docker image and bootstrapping schema takes 10–20s on first run. Subsequent runs in the same session reuse the container.
- **Flaky E2E tests are likely.** Playwright tests against a running backend are real network calls; intermittent failures are common. We mitigate with proper waits (`expect(locator).toBeVisible()` not `setTimeout`), retries on failure (max 2), and quarantine of known-flaky tests.

## Alternatives considered

### Alternative A — Unit tests only, no integration tests
**Rejected** because:
- Most real bugs in CRUD apps are integration bugs (wrong SQL, wrong serialization, wrong auth filter order). Unit tests alone provide false confidence.
- Misses the entire learning value of Testcontainers, which is one of the most important modern Java testing tools.

### Alternative B — H2 in-memory DB for tests instead of Testcontainers
**Rejected** because:
- H2 is *not* Postgres. Different SQL dialect, different type behavior, different transaction semantics. Tests pass against H2 and break in prod.
- Tests written against H2 limit you to lowest-common-denominator SQL — can't use JSONB, can't use Postgres-specific features.
- Slower tests claim is overstated: Testcontainers reuses containers across tests in a single JVM (~1s overhead total).

### Alternative C — Run integration tests against a real Neon DB
**Rejected** because:
- Network latency makes tests slow and flaky.
- Tests in CI would all write to the same DB, causing data interference.
- Free Neon tier connection limits would be hit by parallel test runs.

### Alternative D — Cypress instead of Playwright for E2E
**Rejected** because:
- Playwright has caught up to and exceeded Cypress in most respects (multi-browser support, parallelization, better debugging tools, faster).
- Same learning curve; Playwright is the more current/forward-looking choice.

### Alternative E — Skip E2E entirely; rely on manual QA
**Rejected** because:
- This is the path to undocumented regressions. The first time we ship a feature and break the login flow silently, we'll wish we had it.
- Playwright tests for critical flows (signup, login, check-in, password reset) take a few hours to write once and pay back forever.

### Alternative F — TDD (test-driven development) as a strict workflow
**Not adopted** because:
- TDD is a discipline, not a tool. We will *write tests for what we build* but won't strictly write tests before implementation. Pragmatic test-first when it helps clarify the API, test-after when the design is clear.

## Implementation notes

### Backend setup

- Add Testcontainers BOM to `pom.xml`:
  ```xml
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-bom</artifactId>
        <version>1.20.x</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  ```
- Test dependencies: `testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-postgresql`
- `@Testcontainers` class-level annotation + `@Container` field for the Postgres container
- Shared base class `IntegrationTestBase` that boots the container once and reuses across tests
- `application-test.yml` profile points `spring.datasource.url` at the container's JDBC URL

### Frontend setup

- Vitest config in `vite.config.ts` with coverage thresholds
- MSW worker in `frontend/tests/mocks/` for component tests
- Playwright config in `playwright.config.ts`:
  - Base URL = `http://localhost:3000` (Next.js dev server)
  - Backend assumed running at `http://localhost:8080` (or env-configurable)
  - Three projects: chromium, webkit, mobile-chrome
- `e2e/` directory contains specs; `e2e/fixtures/` has reusable setup (logged-in user, etc.)

### CI workflow shape

```yaml
# .github/workflows/backend.yml (sketch)
on:
  pull_request:
    paths: ['backend/**', '.github/workflows/backend.yml']
jobs:
  test:
    runs-on: ubuntu-latest
    services: { ... }  # not needed - Testcontainers spins up its own
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: ./backend/mvnw -f backend/pom.xml verify
      - uses: codecov/codecov-action@v4  # report coverage
```

## What we will write tests for (in priority order)

1. **`lib/streak.ts` (and Java equivalent):** the formal streak grace rules from [ADR-0002](0002-streak-freeze-silent-grace.md). 100% line, 100% branch.
2. **Auth flows:** signup, login, logout, password reset, rate-limiting behavior.
3. **Habit CRUD:** create, edit, archive, reorder.
4. **Check-in operations:** today, yesterday backfill, idempotency (double-tap shouldn't double-count).
5. **Capture lifecycle:** create with/without remind_at, surface on remind day, mark done/dismissed.
6. **Timezone math:** "today" boundary, DST transitions, user changing timezone.

## References

- [Testcontainers for Java docs](https://java.testcontainers.org/)
- [Spring Boot Testing docs](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.testing)
- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- Vladimir Khorikov, *Unit Testing: Principles, Practices, and Patterns* (excellent book; common interview reference)
