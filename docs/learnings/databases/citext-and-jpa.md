# PostgreSQL `citext` with JPA parameters

> A `citext` column enforces case-insensitive uniqueness, but a JPA query can still compare case-sensitively when JDBC binds its parameter as `varchar`.

## Why this matters in Daily

The `users.email` column is `citext`, so Postgres rejects both `Alice@Example.com` and `alice@example.com` as separate users. Our first Spring Data `findByEmail(String)` query nevertheless returned no row when asked for the lowercase version. The generated SQL was `WHERE email = ?`; JDBC bound `?` as `varchar`, so Postgres selected a case-sensitive comparison.

The repository now casts the parameter to `citext` explicitly:

```java
// backend/src/main/java/com/vishalbhapkar/daily/user/UserRepository.java
@Query(value = "SELECT * FROM users WHERE email = CAST(:email AS citext)", nativeQuery = true)
Optional<User> findByEmail(@Param("email") String email);
```

`UserRepositoryTest` runs against Postgres and checks both mixed-case lookup and case-insensitive uniqueness. An in-memory database would not catch this behavior.

## Interview framing

**Q: Why test a repository against the real database?**

**A:** ORM-generated SQL and database-specific types can interact in unexpected ways. Here, `citext` enforced uniqueness correctly, but a parameterized lookup used a different equality operator. A Postgres integration test found the mismatch before login was built on top of it.

## Gotcha

`@Column(columnDefinition = "citext")` describes the entity column; it does not make every Java `String` query parameter a `citext` value. Inspect the generated SQL and test the actual lookup behavior. PostgreSQL's [`citext` documentation](https://www.postgresql.org/docs/current/citext.html) explains its comparison operators and casts.

*Note created: 2026-09-26*
