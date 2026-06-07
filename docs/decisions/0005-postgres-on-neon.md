# ADR-0005 — Use Postgres on Neon for the database

**Status:** Accepted
**Date:** 2026-06-07
**Decision makers:** Vishal Bhapkar
**Relates to:** [ADR-0004](0004-tech-stack.md) (tech stack)

---

## Context

The app needs a relational database for users, habits, check-ins, captures, and email verification tokens. The choice has two parts:

1. **Database engine:** SQL (Postgres / MySQL / SQLite) vs. document (MongoDB) vs. other
2. **Hosting provider:** managed cloud service, self-hosted, or PaaS-attached

Constraints:
- **Hard requirement:** Free tier, no card on file, free forever for our scale
- **Real production-grade**, not a toy DB (rules out file-based SQLite in production)
- **Compatible with Spring Boot + JPA/Hibernate** (rules out exotic stores)
- **Owner's stated SQL learning goal** — Vishal explicitly chose SQL over NoSQL and accepts that Postgres vs MySQL SQL is ~95% identical

Initial conversation considered MySQL on TiDB Cloud Serverless because Vishal mentioned MySQL specifically. After discussion, Vishal correctly identified that SQL knowledge transfers between MySQL and Postgres, and that reusing the existing Neon Postgres setup is pragmatic.

## Decision

**Database engine:** PostgreSQL (latest stable, currently 16.x)

**Hosting provider:** Neon (serverless Postgres). We reuse the existing Neon project (renamed from `demo-todolist`) that was originally set up for the demo todo apps. See *Why a shared project, not separate projects per app?* below for the rationale.

**Isolation strategy:** Create a **new database named `daily`** within the existing Neon project (alongside the existing `neondb` that hosts the `todos` demo table). This gives:
- Total schema isolation from demo data
- Same connection URL with only the database name changing
- Single Neon account/dashboard for both projects
- No risk of accidentally affecting demo data during Daily development

**Project renamed** for naming hygiene: the existing Neon project was originally called `demo-todolist`. Since it now hosts the production Daily app as well, Vishal renamed it on 2026-06-07 to a project-neutral name (e.g., `vishal-projects`) reflecting that it's a shared container for multiple of his apps. The connection endpoint did not change.

### Why a shared project, not separate projects per app?

In production-grade setups, each app conventionally gets its own database *project* (not just its own *database*), for clean lifecycle separation. We evaluated this and chose the shared-project approach because of Neon's free tier constraint:

| Free tier permits | Free tier does NOT permit |
|---|---|
| Unlimited databases within a project | More than 1 project per account |

Neon's paid Pro tier ($19/month) bumps the per-account project limit to 10. We're prioritizing the "free forever" constraint over the cleaner-project-separation aesthetic.

This trade-off is acceptable because:
- A separate *database* within the same project provides equivalent runtime isolation (different schemas, different connection strings).
- The only thing we lose is per-project lifecycle independence (e.g., can't delete the demo project without affecting Daily). For a personal scope, this is fine.
- If Daily ever outgrows free-tier needs anyway (>0.5 GB storage), the Pro upgrade would naturally come with the project-isolation upgrade as well.

Mitigation: the project was renamed (see above) so its name accurately reflects that it's a multi-app container.

**Connection string pattern:**
- Demo apps: `postgresql://neondb_owner:****@ep-old-term-aqvxlvha.c-8.us-east-1.aws.neon.tech/neondb?sslmode=require`
- Daily app: `postgresql://neondb_owner:****@ep-old-term-aqvxlvha.c-8.us-east-1.aws.neon.tech/daily?sslmode=require`

**Migrations:** Flyway (versioned SQL migrations in `backend/src/main/resources/db/migration/`). No ORM-managed schema; we own the SQL.

**Local development:** A second Postgres instance runs locally in Docker (see [ADR-0008](0008-docker.md)). Schema is applied via Flyway, identical to production. The local Postgres uses simple credentials (`postgres/postgres`) and is *never* connected to from production code.

**Connection pooling:** Neon's pooler endpoint (`-pooler`-suffixed hostname) when running on serverless or low-conn environments. For long-running Render backend, direct connection with HikariCP is fine (Spring Boot default).

## Consequences

### Positive
- **Free forever** at our scale: Neon free tier is 0.5 GB storage per branch, 24/7 availability for "active" branches. Daily's data will be measured in MB.
- **Modern SQL features:** Postgres has CTEs, window functions, JSONB, generated columns, partial indexes — all relevant for senior interviews.
- **JPA-agnostic:** Spring Data JPA + Hibernate works identically with Postgres and MySQL. If we ever need to swap, it's a one-line dialect change + driver swap.
- **No new accounts:** Reuse the existing Neon account that's already authenticated and known to work.
- **Same SQL learning:** ~95% of SQL knowledge transfers between Postgres and MySQL. For the ~5% that differs, we document in `docs/learnings/postgres-and-sql.md`.
- **Strong consistency, full ACID:** Postgres is a proper relational DB; no eventual-consistency gotchas to design around.
- **Real foreign key constraints:** Unlike TiDB (which historically disabled FKs by default), Postgres enforces FKs out of the box — important for schema integrity.

### Negative
- **Less "MySQL on resume" specifically:** If a recruiter filters strictly for "MySQL", this app shows Postgres. Mitigated because (a) modern hiring teams treat Postgres and MySQL experience as equivalent, (b) Spring Data JPA is engine-agnostic — interviewers care that you understand ORMs, not the specific dialect.
- **Postgres-specific features may tempt over-use:** JSONB, arrays, full-text search are powerful but Postgres-only. We'll prefer standard SQL where possible to preserve portability, document deviations.
- **Neon-specific quirks:** Neon scales compute down to zero after inactivity (Postgres-compatible but with potential cold-start latency on first query after idle). For a long-running Render backend, this is rare; for serverless deploys, the pooler endpoint mitigates.
- **Shared Neon project with demos:** If Vishal ever decides to delete the demo project, he must remember the `daily` database lives there too. We document this in `PROJECTS.md`.
- **Single point of failure:** If Neon has an outage, Daily is down. Acceptable for v1 (Neon's status page shows >99.9% uptime historically). No multi-region strategy until / unless scale demands.

## Alternatives considered

### Engine alternatives

**MySQL (originally proposed)**
- Rejected — Vishal initially asked for MySQL but on discussion agreed that SQL learning transfers, and reusing Neon (Postgres-only) outweighs the marginal benefit of "MySQL on resume". No technical reason to prefer MySQL for this workload.

**SQLite**
- Rejected for production — file-based, single-writer, doesn't fit a horizontally-scalable web app on PaaS. Considered for local dev to avoid Docker dependency, rejected because dev/prod parity is more valuable than the dependency savings (see [ADR-0008](0008-docker.md)).

**MongoDB / Firestore**
- Rejected — our data is naturally relational (users → habits → check-ins). Document stores would force denormalization for joins or pull all related data into one document, neither of which fits the model.

**DynamoDB / other KV stores**
- Rejected — too low-level for the simple CRUD workload; SQL is the right abstraction.

### Hosting alternatives

**Supabase (Postgres)**
- Rejected — excellent platform, bundles auth + storage + realtime, but we don't want their auth (we're DIY'ing per ADR-0001) and don't need realtime in MVP. Would also be a new account and credential to manage. Reusing Neon is simpler.

**Render Postgres**
- Rejected — Render's free Postgres tier expires after 30 days; users must upgrade or migrate. Disqualifies on the "free forever" requirement.

**Railway Postgres**
- Rejected — no truly-free tier any more ($5 monthly trial credit).

**Self-hosted Postgres on a VPS**
- Rejected — operations overhead (backups, upgrades, security patches, monitoring) is real work that Neon handles for us at no cost.

**TiDB Cloud Serverless (MySQL-compatible)**
- Rejected — would have been the choice if MySQL was a hard requirement. Since we pivoted to Postgres, Neon is the obvious incumbent.

**Crunchy Bridge / Aiven Postgres**
- Rejected — paid-only or trial-only free tiers.

## Implementation notes

- **Database creation:** Run `CREATE DATABASE daily;` once in the existing Neon SQL editor before first backend boot. Capture the command in `backend/scripts/bootstrap-neon.sql` (one-time setup, not a migration).
- **Migrations:** Versioned SQL in `backend/src/main/resources/db/migration/V1__init.sql`, `V2__add_captures_table.sql`, etc. Flyway auto-runs on application startup.
- **Connection string in production:** stored as `DATABASE_URL` env var on Render (Spring Boot reads it via `spring.datasource.url`).
- **Connection string in dev:** stored in `backend/.env.local` (gitignored), points at local Docker Postgres `jdbc:postgresql://localhost:5432/daily`.
- **No raw SQL access in production code:** all data access via JPA repositories or `@Query` annotations. Ad-hoc DB queries go through the Neon SQL editor (read-only) or `psql` (with care).
- **Backups:** Neon automatically retains 7 days of point-in-time-recovery on free tier. Acceptable for v1. If we ever store irreplaceable data, evaluate a paid tier with longer retention.

## What goes in the schema (preview — formalized in upcoming schema doc)

```
users        (id, email, password_hash, name, timezone, created_at, ...)
habits       (id, user_id, name, emoji, sort_order, archived_at, ...)
checkins     (id, habit_id, date, done, note, created_at)
captures     (id, user_id, text, remind_at, status, created_at, ...)
password_reset_tokens (id, user_id, token_hash, expires_at, used_at)
sessions     (id, user_id, token_hash, expires_at, ...)  -- if we go session-table route
```

(Detailed schema, indexes, FK strategy, and migration plan will go in a separate `docs/schema.md` once we draft it.)

## References

- [Neon documentation](https://neon.tech/docs)
- [Spring Boot data access guide](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#data)
- [Flyway documentation](https://flywaydb.org/documentation/)
