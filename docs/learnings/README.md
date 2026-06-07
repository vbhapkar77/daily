# Learnings — Study Notes Alongside the Code

> This is Vishal's living notebook for important concepts encountered while building Daily. The intent is dual-use: **(1) reference while building**, and **(2) interview prep**. Every concept gets framed as both "what we used it for in this project" and "how it tends to come up in interviews."

---

## Why this folder exists

Most developers learn concepts in three ways:
1. Reading documentation cold
2. Watching tutorials
3. **Building something and hitting the concept in the wild**

(3) is the strongest. But the lesson fades fast unless written down. This folder is the written record — so the next time Vishal sees a question about "what's a Spring Bean?" in an interview, he has not just the textbook answer but the *concrete example* from his own code.

## Organization

Notes are organized by topic area, not by date. Each note follows a consistent structure (see [TEMPLATE.md](TEMPLATE.md)). When a topic grows too large for one file, it gets split into a folder.

```
learnings/
├── README.md                      ← you are here
├── TEMPLATE.md                    ← copy this when adding a new note
│
├── java/
│   ├── README.md                  ← index for Java topics
│   ├── modern-features.md         ← records, sealed classes, virtual threads (Java 21)
│   ├── collections.md             ← when to use which collection
│   ├── concurrency.md             ← threads, executors, CompletableFuture
│   └── ...
│
├── spring/
│   ├── README.md
│   ├── ioc-and-di.md              ← what is the Spring container
│   ├── boot-autoconfiguration.md  ← how starters work
│   ├── data-jpa.md                ← repositories, entities, JPQL
│   ├── security.md                ← filter chain, authentication, JWT
│   ├── transactions.md            ← @Transactional, propagation, rollback
│   └── ...
│
├── databases/
│   ├── postgres-vs-mysql.md       ← the ~5% that differs
│   ├── indexes.md                 ← when to add, when not
│   ├── transactions-and-isolation.md
│   ├── jpa-n-plus-1.md            ← the classic ORM gotcha
│   └── ...
│
├── testing/
│   ├── pyramid.md                 ← unit / integration / e2e
│   ├── testcontainers.md          ← how & why
│   ├── mocking.md                 ← Mockito patterns, what NOT to mock
│   └── ...
│
├── infrastructure/
│   ├── docker.md                  ← multi-stage builds, layers, networking
│   ├── kubernetes.md              ← if/when we do the side-quest
│   ├── ci-cd.md                   ← GitHub Actions concepts
│   └── ...
│
├── production-concerns/
│   ├── observability.md           ← logs vs metrics vs traces
│   ├── security.md                ← OWASP top 10 we actually care about
│   ├── scaling.md                 ← horizontal vs vertical, caching, queues
│   └── ...
│
├── system-design/
│   ├── rest-api-design.md         ← what we did, why
│   ├── auth-patterns.md           ← cookie vs JWT-header, why we chose cookie
│   └── ...
│
└── interview-questions.md          ← cross-cutting index of "this is how interviewers frame X"
```

The structure grows as topics emerge. Don't create empty stubs — only write a note when you actually understand the concept enough to explain it.

## Note format

Every note follows the structure in [TEMPLATE.md](TEMPLATE.md). Quick summary:

1. **Concept name** + one-line summary
2. **Why it matters** — what problem it solves
3. **How we use it in Daily** — concrete code reference with file path
4. **The textbook explanation** — short, in your own words
5. **Common interview framing** — actual questions, your answer
6. **Gotchas & misconceptions** — the things that trip people up
7. **Related concepts** — links to other notes
8. **Further reading** — books, docs, blog posts

## When to write a note

Write a note when:
- You learn something new while building
- You answer a question someone else might have
- You make a non-obvious choice (and want to remember the reasoning)
- You hit a gotcha and want to record the fix
- You read about something in a book/blog/SO and want to internalize it

Do **not** write a note when:
- You're paraphrasing the Java tutorial (that's reading, not learning)
- You don't understand the concept yet (write the note *after* you understand)
- The concept is so trivial that explaining it adds noise

## Connection to the rest of the docs

- Learnings are **for the human**. Code that documents itself doesn't need a note. Decisions go in ADRs, not here.
- When a concept is core to a decision, the ADR may link to the relevant note for background.
- A note can outlive its origin — even if we someday remove the code that prompted it, the lesson is still valuable.

## Index of current notes

### Java

- [Maven dependencies — scopes, BOMs, the api/impl split](java/maven-dependencies.md) *(S001 T-001)*

### Infrastructure

- [GitHub Actions: required checks vs. paths-filtered workflows](infrastructure/github-actions-required-checks.md) *(PR #2 — classic CI/CD interview topic)*
