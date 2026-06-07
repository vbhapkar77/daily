# ADR-0011 — Use Java 25 (LTS) instead of Java 21

**Status:** Accepted
**Date:** 2026-06-07
**Supersedes:** Part of [ADR-0004](0004-tech-stack.md) (the Java version choice; everything else in ADR-0004 stands)
**Decision makers:** Vishal Bhapkar

---

## Context

ADR-0004 specified "Java 21 (LTS)" as the backend language version because Java 21 was the latest LTS at the time of that ADR's first draft. While setting up the development environment, we discovered Vishal's machine already has **Java 25 (LTS)** installed (released October 2025 — newer than Java 21 and also an LTS release).

Java release cadence reminder:
- Java 17 LTS — Sept 2021
- Java 21 LTS — Sept 2023
- Java 22 / 23 / 24 — non-LTS releases (Mar 2024, Sept 2024, Mar 2025)
- **Java 25 LTS — Sept 2025** ← the latest LTS as of this decision
- Java 26 non-LTS — Mar 2026 (current latest non-LTS)

Spring Boot 3.x supports Java 17 and up, so Spring Boot 3 runs cleanly on Java 25.

Rather than downgrade to Java 21 to match the original ADR, the cleaner path is to update the project to target Java 25 — which is what the original ADR's rationale called for ("latest LTS, modern Java features").

## Decision

The project's backend targets **Java 25 LTS** instead of Java 21 LTS.

Concretely:
- `pom.xml`: `<java.version>25</java.version>`, `<maven.compiler.source>25</maven.compiler.source>`, `<maven.compiler.target>25</maven.compiler.target>`
- `Dockerfile`: `eclipse-temurin:25-jdk-alpine` (build stage), `eclipse-temurin:25-jre-alpine` (runtime stage)
- GitHub Actions `setup-java@v4`: `java-version: '25'`, `distribution: 'temurin'`

All other choices in ADR-0004 (Spring Boot 3, Maven, JPA, etc.) remain unchanged.

## Consequences

### Positive
- **Two years more recent** than Java 21. Same LTS guarantees.
- Access to features added between Java 22 and 25: scoped values, generational ZGC, stream gatherers, refined pattern matching, etc. These come up in modern senior interviews.
- No need to install a second JDK on the machine; Vishal's existing JDK 25 install is what we use.
- Matches "use the latest LTS" principle that the original ADR's rationale implied.

### Negative
- Some third-party libraries may not yet have explicit Java 25 support listed in their docs (even if they work). We'll watch for compatibility issues with: Testcontainers, Flyway, Spring Boot itself. All three confirm Java 25 support as of 2026-Q2.
- A small number of bytecode-manipulation libraries (older instrumentation, agents) lag JDK releases. Unlikely to affect our stack but worth knowing.
- Marginally smaller pool of online tutorials / Stack Overflow answers specific to Java 25. The Java 21 corpus is still applicable for >95% of questions.

## Alternatives considered

### Alternative A — Downgrade to Java 21 (match ADR-0004 literally)
**Rejected** because:
- Requires installing a second JDK on the machine.
- The rationale in ADR-0004 was "latest LTS" — Java 25 IS the latest LTS now. Updating fulfills the rationale; downgrading violates it.
- Java 25 is at least as well-supported by our stack as Java 21.

### Alternative B — Java 26 (non-LTS, current latest)
**Rejected** because:
- Non-LTS releases get only 6 months of free Oracle support. LTS gets 5 years.
- Production projects should pin to LTS. Following our own discipline.

### Alternative C — Just silently use Java 25 without an ADR
**Rejected** because:
- We have a rule: "decisions are append-only; if a decision changes, write a new ADR that supersedes the old one."
- Quietly contradicting an accepted ADR sets a bad precedent — even for trivial changes.
- The 5 minutes to write this ADR pays back in process discipline.

## Implementation notes

- All Dockerfile JDK image tags change to `25` from `21`. Image size is roughly equivalent (Alpine JRE images for both are ~140-160MB).
- IntelliJ Project Structure → Project SDK should be set to 25. Module language level: 25.
- The `application.yml`'s `spring.main.banner-mode` and other Spring properties unaffected.

## What does NOT change

The rest of ADR-0004 is unaffected:
- Spring Boot 3.x ✓
- Maven (with `./mvnw`) ✓
- Spring Data JPA + Hibernate ✓
- All other dependencies ✓

## Change log

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 2026-06-07 | Vishal + Claude | Java target updated 21 → 25 LTS based on what's already installed |
