# Maven dependencies — scopes, BOMs, the api/impl split

> How Maven manages library versions and where they're visible on the classpath. Covers the api/impl split pattern we just hit with JJWT, and the scope rules that decide whether code can `import` something at compile time vs only at runtime.

---

## Why this exists / the problem it solves

Every non-trivial Java project depends on dozens of libraries. Without rules, you'd hit:

- **Version conflicts:** library A wants `commons-lang 2.6`, library B wants `commons-lang 3.14` — which wins?
- **Bloated runtime image:** if you ship every dep your tests need into production, your container is 4× bigger than necessary.
- **Leaky implementations:** application code accidentally calling internal classes of a library, then breaking when the library's internals change.

Maven (and Gradle) solve these with three concepts: **scopes**, **transitivity rules**, and the **BOM (bill of materials)** pattern.

---

## How we use it in Daily

Our `pom.xml` declares dependencies in `<dependencies>` with optional `<scope>` and `<version>` overrides. Recent example from `backend/pom.xml` after adding JJWT (S001 T-001):

```xml
<properties>
  <jjwt.version>0.12.6</jjwt.version>   <!-- version centralized -->
</properties>

<dependencies>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
    <!-- default scope is `compile` — visible at compile, test, and runtime -->
  </dependency>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>      <!-- NOT visible to your code's imports -->
  </dependency>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
  </dependency>
</dependencies>
```

Why this exact shape:

1. **`jjwt-api` at `compile` scope:** our code does `import io.jsonwebtoken.Jwts;` — that interface lives in the api jar. By depending only on api, our code is decoupled from the implementation.
2. **`jjwt-impl` at `runtime` scope:** this is the actual JWT signing/verifying code. Our code *never* imports from this jar. Maven still puts it on the runtime classpath so it can be loaded reflectively by `Jwts.builder()` at runtime. But the compiler can't see it — which is exactly what we want.
3. **`jjwt-jackson` at `runtime` scope:** an adapter that wires JJWT into Jackson (which Spring Boot uses for JSON). Same principle as `jjwt-impl`.

This is the **api/impl split** pattern. Lots of well-designed libraries use it: SLF4J (`slf4j-api` + `slf4j-simple` or `logback-classic`), JJWT, jakarta validation API, etc.

---

## The concept (in your own words)

### Scope decides "where on the classpath this dependency lives"

| Scope | Compile classpath | Test classpath | Runtime classpath | Use it for... |
|---|---|---|---|---|
| `compile` *(default)* | ✓ | ✓ | ✓ | Most things. Anything your `src/main` code imports. |
| `provided` | ✓ | ✓ | ✗ | Code that imports it at compile but the runtime environment provides it (e.g., `servlet-api` if you deploy to Tomcat). |
| `runtime` | ✗ | ✓ | ✓ | Implementation libs that are wired by reflection / SPI. Your code shouldn't import them — only the runtime should load them. |
| `test` | ✗ | ✓ | ✗ | JUnit, Mockito, Testcontainers. Never shipped to production. |
| `system` | ✓ | ✓ | ✓ | Legacy — a jar from your local filesystem. Avoid. |
| `import` | n/a | n/a | n/a | Special. Only used with `<dependencyManagement>` to import a BOM. |

The two most common ones — `compile` and `test` — cover ~80% of real projects. `runtime` is the sleeper that matters for clean architecture.

### Transitive dependencies — Maven follows them automatically

If you depend on `spring-boot-starter-web`, Maven pulls in `spring-web`, `spring-mvc`, `jackson-databind`, `tomcat-embed-core`, and ~30 other jars without you listing them. This is **transitive dependency resolution**.

The rule: **transitive deps inherit a "narrowest" scope.** If you import a `test`-scoped dep, you don't get its transitive deps at compile time. If you import a `compile`-scoped dep, you DO get its transitive `compile` deps at compile time — but its `runtime` transitive deps come in at runtime.

### Version conflicts — "nearest wins"

When two paths in the dep graph want different versions of the same library, Maven applies the **nearest-wins rule** — the version closest to the root of the tree (i.e., fewest hops from your `pom.xml`) wins.

This is brittle. The fix: explicitly declare the version you want in `<dependencyManagement>` (or via a BOM, see below). Then everyone's transitive copy gets aligned to that version.

### BOM (Bill of Materials) — centralize version management

A BOM is a special `pom.xml` whose only job is to declare versions for a coordinated set of artifacts. Spring Boot's `spring-boot-dependencies` BOM is the most famous: when you `<parent>` it, every Spring jar magically gets the right compatible version without you writing `<version>` everywhere.

Example pattern:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>testcontainers-bom</artifactId>
      <version>1.20.4</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <!-- no version! Inherited from the BOM -->
    <scope>test</scope>
  </dependency>
</dependencies>
```

This is the **idiomatic** way to manage multi-artifact libraries. We don't need it for JJWT because we have only 3 artifacts and they share a property already, but for larger families (Spring, Jackson, AWS SDK), BOMs are the norm.

### The api/impl split — defensive library design

When a library author splits their distribution into `*-api` and `*-impl`, they're sending two messages:

1. **"Depend only on -api in your source code."** The api jar contains interfaces, factory methods, and stable types. The impl jar contains private classes that may change without notice.
2. **"Put -impl on the runtime classpath."** The api can't function without an implementation — it loads one at runtime via Java's ServiceLoader (or similar). You bring the impl in via `<scope>runtime</scope>` so the compiler can't accidentally let your code import private internals.

Benefits:
- **You can swap implementations.** SLF4J is the king of this: same `LoggerFactory.getLogger(...)` call works whether the impl is Logback, Log4j2, or `java.util.logging`.
- **The api is a smaller, more stable contract.** Bug fixes in the impl don't force you to change your code; the api signatures stay the same.
- **It enforces good architecture in your own code.** You can't accidentally couple to internals.

Not every library does this — many ship a single jar. But when you see it, recognize it as a deliberate architectural choice.

---

## Common interview framing

> **Q: "What are Maven scopes? Walk me through the differences."**
> A: Scopes decide which classpaths a dependency is available on — compile, test, and runtime are the most important. `compile` (default) puts the dep on all three. `test` puts it only on the test classpath — JUnit and Mockito are classic examples. `runtime` is the interesting one: it's on the test and runtime classpaths but **not** the compile classpath, so your `src/main` code can't `import` from it. This is used for implementation libraries that are wired by reflection or SPI — like JDBC drivers, or JWT implementations. `provided` is used when the runtime environment supplies the jar (e.g., the servlet-api when deploying to a real Tomcat).

> **Q: "Two of your dependencies want different versions of the same library. What happens? How do you control it?"**
> A: Maven applies the "nearest-wins" rule — the version closest to the root of the dependency tree wins. This is fragile because adding a new dep can silently change which version of a transitive library you end up with. The fix is to declare the version explicitly in `<dependencyManagement>`, or — better — to import a BOM (Bill of Materials) that coordinates a known-good set of versions across a whole library family. Spring Boot's parent POM is essentially a giant BOM; that's why you don't write `<version>` on Spring deps.

> **Q: "Why does JJWT (or SLF4J) split into api and impl jars?"**
> A: It's the api/impl pattern. You put `jjwt-api` at `compile` scope so your code can import `Jwts`, `Jws<Claims>`, etc. — the stable interface. You put `jjwt-impl` (and a JSON-parser adapter like `jjwt-jackson`) at `runtime` scope. Your compiler can't see anything from `-impl`, so you physically can't accidentally couple to internal classes. The api uses Java's ServiceLoader to find and load the impl at runtime. It's a small detail but it forces good architecture and lets the library author refactor internals freely.

> **Q: "What's `mvn dependency:tree` and when do you use it?"**
> A: It prints the full transitive dependency graph as a tree, with version conflicts marked. I run it whenever I get unexpected behavior at runtime — usually it reveals a version conflict where a transitive dep brought in an older copy of something. Combined with `dependency:analyze`, you can also spot unused declared deps and "used but undeclared" deps that you should add explicitly.

---

## Gotchas & misconceptions

- **`runtime` scope is not "lazy loading."** It's a compile-time visibility rule. The jar is in the same uberjar at deployment time as compile-scoped deps; it's just hidden from the compiler.
- **`<scope>test</scope>` deps are NOT shipped to production.** Common newcomer confusion: they think they need to switch to `compile` to make production work. They don't — the test classpath is for tests, not production.
- **The `${jjwt.version}` property must be defined in `<properties>` or you'll get an unresolvable build.** A common error is to add a dep referencing a property you haven't declared.
- **Spring Boot's `<parent>` provides a managed version for many things — don't add `<version>` for Spring artifacts.** It overrides the managed version and can cause subtle incompatibilities.
- **`provided` scope is rarely needed for Spring Boot apps** because we ship the embedded server inside the jar. It mostly comes up in old WAR-deployment scenarios.

---

## Related concepts

- See [[spring/boot-autoconfiguration]] (coming later) for how Spring uses transitive deps to wire features automatically when starters are on the classpath.
- See [[testing/testcontainers]] (coming later) for the BOM pattern in action with `testcontainers-bom`.

---

## Further reading

- [Maven docs — Dependency Scopes](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Scope) — primary source, short read
- [SLF4J's manual on api/impl pattern](https://www.slf4j.org/manual.html) — clearest example of why this design works
- *Maven: The Complete Reference* (Sonatype, free online) — chapters 9 and 10 cover this thoroughly
- [`mvn dependency:tree` docs](https://maven.apache.org/plugins/maven-dependency-plugin/tree-mojo.html)

---

*Note created: 2026-06-08 during S001 T-001 (adding JJWT to backend).*
