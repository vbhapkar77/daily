# Discussion — Spotless disabled on JDK 25

**Date:** 2026-06-07
**Status:** Temporary measure
**Owner:** Vishal

## What happened

While scaffolding the backend in Phase 2, we added the Spotless Maven plugin (per [AGENTS.md](../../AGENTS.md) conventions: "Match the formatting tools (Spotless for Java...)"). Both formatter backends we tried failed identically when run under JDK 25:

```
java.lang.NoSuchMethodError: 'java.util.Queue com.sun.tools.javac.util.Log$DeferredDiagnosticHandler.getDiagnostics()'
```

This is a known upstream issue: JDK 25 changed the internal `com.sun.tools.javac` API that both `google-java-format` and `palantir-java-format` use to parse and rewrite Java source. As of June 2026, neither formatter has shipped a release with the fix.

Tried (all failed identically):
- google-java-format 1.22.0 (AOSP style)
- palantir-java-format 2.50.0 (PALANTIR style)
- Spotless 2.43.0 plugin version

## What we decided

**Disable Spotless auto-execution** in `pom.xml` (the `<executions>` block is commented out, plugin block stays so manual invocation is still possible). **Remove the Spotless step** from `.github/workflows/backend.yml`.

The plugin block is preserved so re-enabling is a one-comment-change away — uncomment the `<executions>` block in `pom.xml` and restore the lint step in `backend.yml`.

## Why this is acceptable

- It's a single-developer project at the moment; formatting drift is bounded.
- IntelliJ IDEA enforces consistent formatting via its built-in formatter as long as we use AOSP / Palantir-like settings.
- The plugin is still resolvable, so when the upstream fix lands, re-enabling takes seconds.

## What we'll do instead in the interim

- **IntelliJ formatter** does the heavy lifting on save. Configure via Preferences → Editor → Code Style → Java → "Set from..." → Palantir or AOSP style preset.
- **Editorconfig** (`.editorconfig` at repo root) enforces the obvious — indentation, trailing whitespace, line endings.
- **Code review** catches stylistic drift on PRs.

## When to revisit

Watch for releases of:
- google-java-format 1.29+ or whatever ships JDK 25 support (https://github.com/google/google-java-format/releases)
- palantir-java-format 2.51+ (https://github.com/palantir/palantir-java-format/releases)
- Spotless 2.46+ that bundles a JDK 25-compatible formatter (https://github.com/diffplug/spotless/releases)

When any of those land:
1. Update version in `pom.xml`
2. Uncomment the `<executions>` block
3. Restore the `Lint (Spotless check)` step in `.github/workflows/backend.yml`
4. Run `./mvnw spotless:apply` once to align existing code
5. Delete this discussion file (or move to an archived note)

## Lessons

- Plugins that depend on internal JDK APIs (`com.sun.tools.javac.*`) tend to lag JDK releases. This is true even for high-quality plugins from Google / Palantir.
- When choosing the bleeding-edge LTS (Java 25), expect some ecosystem catch-up time. The benefits (newer JIT, security patches) typically outweigh, but a few tools may need workarounds.
- "Disable temporarily, document the why, set a re-enable trigger" is the right play. Better than removing the plugin and forgetting, better than blocking on the fix.
