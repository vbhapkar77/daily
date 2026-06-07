-- Run this ONCE on Neon to create the `daily` database alongside the existing `neondb`.
-- Run from Neon's SQL editor while connected to the default `neondb` database.
--
-- Note: this is not a Flyway migration (those run per-database; this creates the
-- database itself). It's a one-time bootstrap.

CREATE DATABASE daily;

-- Then connect to the new `daily` database and verify:
--   SELECT current_database();   -- should return: daily
