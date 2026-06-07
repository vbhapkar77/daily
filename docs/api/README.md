# API contract

This folder holds the **HTTP API contract** for Daily. The contract is the source of truth:

- The Spring controllers conform to the contract (not the other way around)
- The frontend's TypeScript client is **generated** from this contract on each build
- Documentation (Swagger UI) is served from this contract
- Optional: contract tests can be run against the live backend to verify it matches

This is contract-first SDD, formalized in [ADR-0010](../decisions/0010-adopt-sdd-practices.md).

## Files

| File | Purpose |
|---|---|
| [`openapi.yaml`](openapi.yaml) | The canonical, full OpenAPI 3.1 specification |
| `.spectral.yaml` (later) | Spectral linter config — style + consistency rules |

Per-feature OpenAPI fragments live in `specs/NNN-feature-name/api.yaml`. They get merged into `openapi.yaml` as the feature is approved.

## Tooling

| Tool | Purpose | Status |
|---|---|---|
| **SpringDoc** | Serves Swagger UI in the backend at `/swagger-ui.html`, reading our static YAML | Will be set up in Phase 2 |
| **openapi-typescript** | Generates TS types for the frontend (`frontend/lib/api-types.ts`) | Will be set up in Phase 2 |
| **Spectral** | Lints the YAML for style + consistency | Will be set up in Phase 2 |
| **Prism** (optional) | Local mock server from the YAML for frontend dev without backend | Optional |

## How to edit

When adding/changing an endpoint:

1. Write the change in the relevant `specs/NNN-feature/api.yaml` fragment (if it's part of a new feature)
2. Merge into `openapi.yaml` (manual for now; could be scripted later)
3. Run Spectral lint locally: `spectral lint docs/api/openapi.yaml`
4. Regenerate frontend types: `cd frontend && pnpm openapi:gen`
5. Implement the controller to match (backend tests will fail if it doesn't)
6. PR includes both the spec change and the implementation

## Versioning

- The base path includes a version: `/api/v1/...`. Breaking changes get a new version (`/api/v2/...`).
- Within a major version, additive changes only (new endpoints, new optional fields). No breaking changes without a new version.
- We do not currently expose multiple versions simultaneously (single deployment, single client). When we ship a new version, the old one is retired in the same release.

## Linter rules (Spectral)

Once configured, `.spectral.yaml` will enforce:

- All operations have `summary`, `description`, `operationId`
- All responses have a `description`
- Path naming: kebab-case, plural nouns (`/users`, not `/User`)
- All schemas are defined under `components/schemas` (no inline schemas)
- Required fields explicitly listed
- All `4xx` and `5xx` responses reference our common `Error` schema

## Where the contract is "consumed"

```
                  docs/api/openapi.yaml
                          │
            ┌─────────────┼──────────────┐
            │             │              │
            ▼             ▼              ▼
   Backend (SpringDoc)  Frontend     Tooling (Spectral
   serves Swagger UI    (codegen     lint, optional
   at /swagger-ui.html  api-types)   contract tests)
```
