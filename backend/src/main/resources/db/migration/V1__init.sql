-- V1: bootstrap schema
-- Per docs/architecture.md §4 schema preview. Full domain tables are added per-feature
-- (e.g., V2 adds users + auth, V3 adds habits, etc.).
--
-- For v1 init: just ensure the citext extension is available for case-insensitive emails.

CREATE EXTENSION IF NOT EXISTS citext;
