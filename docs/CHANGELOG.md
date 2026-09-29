# ZPantry Java Migration Documentation Changelog

## 2026-09-29 — Audit security and pantry validation repairs

- Restricted JSON/v2 catalog mutations and media upload/delete to `SUPER_ADMIN`, `ADMIN`, or `MANAGER`; anonymous and ordinary users are rejected by the security filter chain.
- Ingredient creation now persists before its best-effort embedding call, so generated UUIDs are available and an unavailable AI adapter cannot abort the CRUD transaction.
- Pantry create/update now requires an active ingredient, positive quantity and nonblank unit. JSON update distinguishes an omitted `expiredAt` (leave unchanged) from explicit `null` (clear it).
- Owner-authorization exceptions now map to HTTP 403 for profile controllers, and duplicate registration maps to 409 instead of 500.
- The external AI/media/Today Menu detail/completion findings remain unverified and are not marked resolved.

## 2026-09-28 — Super Admin Authority

- Super Admin now inherits all User administration read/delete permissions and may manage or assign every application role, including `SUPER_ADMIN`, as an explicit product policy.

## 2026-09-28 — Vietnamese Development Recommendation Catalog

- Added an idempotent, `dev`-profile-only catalog seed with common Vietnamese ingredients and
  nine recipes, including allergen metadata and recipe-ingredient links.
- Added English aliases and `Egg Tomato Rice Bowl` so the existing AI test account can exercise
  Pantry-based Recommendation V2 end-to-end.
- Verified `POST /api/recommendations/v2/meals` returns two ranked results for `topK: 2`.

## 2026-09-25 — Shared Development Stack Port Allocation

- Reserved `15432` for the Docker PostgreSQL/pgvector service, leaving the host PostgreSQL
  listener on `5432` untouched.
- Standardized backend-to-AI development traffic on port `8000` and backend HTTP on `8080`.
- Added the workspace-level Compose stack for PostgreSQL, AI, backend and mobile web; the landing
  frontend is intentionally excluded. Mobile web maps to `18081` to avoid the active Expo `8081`.

## 2026-09-23 — Development Startup Configuration Repair

- Restored the Docker-backed datasource settings to the active `dev` profile
  (`localhost:54329`, `zpantry_dev`) instead of the unrelated local `postgres` defaults.
- Registered the profile-scoped development email-verification adapter so the Authentication
  service can be created during a `dev` startup.
- Verified a clean dev startup connects to PostgreSQL 16/pgvector, applies/validates Flyway,
  starts Tomcat on port 8080, and serves `/v3/api-docs` with HTTP 200.

## 2026-09-19 — dev Startup Datasource Fix

- Added a default dev profile and dedicated PostgreSQL 16/pgvector Docker Compose service.
- Added optional Spring Boot Docker Compose development support.
- Verified a no-profile Maven/IDE launch runs Flyway V1, validates Hibernate and starts Tomcat.

## 2026-09-19 — Clean Database Flyway Baseline and Test Seed

- Added V1 for pgvector and all 13 mapped ZPantry tables.
- Added explicit `fresh` activation while keeping Flyway and baselining disabled by default.
- Removed drifting `foundation.sql`; persistence tests now migrate empty containers.
- Added deterministic synthetic seed data and migration/catalog/idempotency/seed tests.
- Documented clean bootstrap and blocked adoption for existing EF databases.

## 2026-09-19 — System-wide Verification Pass Started

- Re-ran the Java 21 verification lifecycle against isolated PostgreSQL 16 + pgvector.
- Corrected Ingredient search so filtering and totals are computed in PostgreSQL before paging.
- Restored v2 Ingredient update image handling and legacy duplicate-name rejection.
- Added PostgreSQL regression coverage for filtered pagination, multipart image persistence and
  duplicate rename behavior.
- Recorded confirmed Recommendation contract/persistence and Today Menu detail/compensation gaps;
  no affected module was promoted to complete.

## 2026-09-19 — Batch Production Implementation

- Added all remaining legacy controller routes plus Authentication, JPA domain mappings, media and
  AI adapters, pgvector persistence and Today Menu multi-entity consumption behavior.
- Fixed final-run failures involving RestClient/Jackson wiring, password verification argument
  order, test revocation bean ambiguity and PostgreSQL vector binding via `hibernate-vector`.
- Java 21 `clean verify` passed 8 unit and 13 integration tests with no failures or skips.
- New modules remain IMPLEMENTED_NOT_VERIFIED pending full legacy HTTP parity and configured
  Cloudinary/AI environment verification.

## 2026-09-19 — User Vertical Slice Implemented

- Accepted ADR-012 Option B and implemented deterministic validated owner UUID extraction without
  reproducing the legacy email-`sub` shadowing defect.
- Added the exact 20-column UUID/Instant User mapping, soft-delete repository queries, legacy
  pagination, response/request records, service rules and four `/api/users` operations.
- Added a narrow HS256 legacy JWT consumer, normalized role mapping, immutable authenticated-user
  model, fail-closed configuration and required `TokenRevocationChecker` boundary. Authentication
  issuance/login/refresh/logout/OTP were not migrated.
- Added ASP.NET Identity v2 PBKDF2-HMAC-SHA1 generation/verification with secure random salts and
  the documented 49-byte marker-0 payload.
- Added unit and isolated PostgreSQL/API/security tests, including real signed JWT validation and
  the claim-shadowing regression. Production cutover remains blocked on shared schema/revocation.
- Microsoft OpenJDK 21.0.12 `clean verify` passed 8 unit and 11 integration tests with zero
  failures, errors or skips; PostgreSQL tests used only disposable Testcontainers databases.

## 2026-09-19 — User ClaimsPrincipal Diagnosis

- Reproduced matching-owner PUT 403 using a token from the real disposable legacy login flow and
  captured exact redacted request/response, raw token claims and validated principal claims.
- Proved JwtBearer 10.0.9 uses `JsonWebTokenHandler`/IdentityModel 8.0.1 with inbound mapping on;
  email `sub` becomes the first NameIdentifier and shadows the later UUID-valued claim.
- Reproduced the failure with an independently signed evidence-backed token using email `sub` and
  UUID `nameid`; classified the behavior as a legacy claim-mapping bug.
- Documented strict-compatibility Option A and recommended bug-fix Option B. Added proposed
  ADR-012; no option was implemented and MIG-004 remains PENDING.
- Reclassified User implementation as READY WITH DOCUMENTED LIMITATIONS while keeping production
  cutover BLOCKED on the actual shared users catalog and revocation interoperability.
- Added a diagnostic capture script and redacted fixtures. No Java production/test/build code,
  Foundation component, database schema or legacy repository changed; Maven was not rerun.

Keep entries concise and focused on meaningful migration changes.

## 2026-09-18 — Disposable Legacy User Contract Capture

- Reverified Foundation on JDK 21 with Docker: Maven verify passed 4 unit + 3 PostgreSQL/pgvector
  integration tests, zero failures/errors/skips; no Foundation change was needed.
- Built the pinned net10.0 legacy backend in Docker and ran it against private PostgreSQL
  16.14/pgvector with test-only JWT/email settings and three synthetic bootstrap accounts.
- Captured 23 raw User HTTP cases with headers, provenance and before/after database rows; no
  bearer/refresh tokens, real credentials, real data or external service calls were persisted.
- Confirmed the official migration creates the documented 20-column users table and that admin
  delete changes only is_deleted/deleted_at; actual shared-database evidence remains unavailable.
- Confirmed list/detail/delete/pagination and middleware auth responses. Discovered that real
  login-issued owner tokens always receive the explicit PUT 403, leaving source-defined update
  semantics unreachable. Recorded this source/runtime conflict as the implementation blocker.
- Accepted ADR-008 for exact temporary legacy hash generation during shared-table coexistence.
  No Java User/Auth production code or API/database redesign was introduced.

---

## 2026-09-18 — User Evidence Closure

- Attempted a safe actual-schema probe. dev PostgreSQL 17 required unavailable credentials;
  no session/SQL/database modification occurred. Actual users catalog remains BLOCKED.
- Could not safely execute the net10.0 legacy host: only .NET SDK 9 was installed, Docker was
  unavailable and no isolated legacy database/configuration existed. Added an evidence README
  instead of fabricated HTTP JSON; contract capture remains BLOCKED.
- Executed Microsoft.AspNet.Identity.Core 2.2.4 with a synthetic password and independently
  verified its generated hash on JDK 21: version marker 0, PBKDF2-HMAC-SHA1, 1,000 iterations,
  16-byte salt, 32-byte subkey and 49-byte payload. Added a safe regression vector and selected
  temporary exact legacy generation/verification as the recommended strategy.
- Reverified NameIdentifier/Role URI claims, UUID owner parsing and the no-admin-bypass PUT rule.
  Claim evidence is source-complete, but Java JWT/revocation consumption remains unimplemented.
- Classified User implementation BLOCKED on actual-table and live HTTP evidence. Foundation
  status is unchanged; no User/Authentication Java code or schema/API redesign was introduced.
- Java compilation passed on Microsoft JDK 21.0.12 before documentation/evidence updates.
- Final JDK 21 clean verify passed 4 unit tests and failed the 3 existing integration tests
  because Testcontainers could not find the currently unavailable Docker engine. No test was
  skipped/disabled or changed; prior successful Foundation evidence/status remains intact.

---

## 2026-09-18 — User Migration Analysis Only

- Confirmed existing API_MIGRATION.md is present/readable; resolved the prior missing-file
  blocker. Labeled its original examples and added source-derived MIG-004 as PENDING.
- Inspected pinned User/entity/repository/service/controller/DTO code, Authentication field
  writes and claims, host behavior, EF migration and conflicting users SQL definitions.
- Documented all 20 proposed columns, UUID/audit/soft-delete behavior and the missing live
  schema evidence. Added implementation file plan, security dependencies and unit/PostgreSQL/
  API parity test gates in USER_MIGRATION_ANALYSIS.md.
- Marked User ANALYZING, not implemented/COMPLETE; recorded R-020 through R-023 for schema,
  password writer compatibility, authorization dependencies and uncaptured runtime behavior.
- No User/Authentication code, Foundation changes, schema writes or fabricated response
  fixtures. Maven was not rerun for this documentation-only analysis; prior Foundation
  verification remains the recorded evidence. Implementation awaits explicit approval.

---
## 2026-09-16 — Foundation Verification Passed; API Document Located

- Ran full Maven verify on Microsoft JDK 21.0.12: BUILD SUCCESS, 4 unit tests and 3 integration
  tests, zero failures/errors/skips. No implementation/configuration fixes were needed.
- Confirmed Docker Npipe detection and pinned PostgreSQL 16.14 / pgvector 0.8.2 startup,
  exact container datasource URL, JPA UUID/numeric/date/Instant persistence, vector(1536)
  round-trip and invalid dimension rejection. No external database fallback or legacy writes.
- Hibernate validate, disabled SQL initialization/Flyway, and absence of Flyway history
  verified. Testcontainers resources were cleaned up after Maven exited.
- Marked Foundation readiness COMPLETE for infrastructure scope, not legacy module parity;
  resolved R-013/R-019 and updated migration status, risks, test strategy and schema notes.
- Located API_MIGRATION.md in Downloads and confirmed it is byte-identical to Git's staged
  6300-byte blob. The working-tree file remains deleted (AD), not untracked; no other registered
  worktree or committed version exists. Deletion intent is unknown. No replacement or restore.
- No API migration status changed and no User/business migration started.

---
## 2026-09-16 — Foundation Readiness Implementation

- Enforced Java 21 and compiler release 21; verified on Microsoft JDK 21.0.12 and confirmed
  default JDK 23 rejection. Kept Boot 4.0.8 and wrapper Maven 3.9.16.
- Moved the composition root to com.zpantry; added source-derived response/error/page records
  and explicit Bean Validation without business endpoints or speculative error/tracing policy.
- Set Hibernate validate and disabled SQL initialization/Flyway execution, clean and automatic
  baselining. Added private-configuration ignore rules. Removed unused Spring AI vector store.
- Added container-owned PostgreSQL 16/pgvector 0.8.2 integration tests with digest-pinned image,
  disposable SQL probe and no external database fallback. Replaced the empty smoke test.
- Added documented feature contract-fixture directories without fabricated endpoint payloads.
- Unit checks pass on Java 21. Full verify reaches Failsafe but fails with three context errors
  because Docker is unavailable; database assertions remain unverified. No legacy DB contacted.
- Recorded ADR-009/010/011, updated architecture/schema/testing/status/risk documentation,
  and left Foundation incomplete pending integration verification. No API_MIGRATION.md status
  change, legacy schema mutation, feature implementation or next-slice work.

---
## 2026-09-16 — Java Baseline Audit

- Replaced unverified migration states with the actual scaffold inventory: one entry point, one failing context test, no implemented feature modules and no verified API/database parity.
- Verified Boot 4.0.8 and Java release 21 in the build; recorded Maven 3.9.16 running JDK 23.0.2 versus Java 8 on PATH.
- Ran clean verify and a separate test invocation: both failed with 1 test error caused by missing datasource configuration. Packaging with tests explicitly skipped succeeded; dependency inspection confirmed transitive validation support and database/vector libraries.
- Read the staged API_MIGRATION.md because its working-tree file is missing. Recorded that its migration entries are examples, not delivered features; preserved the existing staged/deleted state without creating a replacement.
- Rechecked legacy main at a010fdc5894176596bb195e4fef66db2c09496f1 and inspected selected controllers, response type, EF mappings and vector-search stub without fetching secret configuration.
- Added risks R-012 through R-018 covering missing API documentation, startup/test failure, JDK drift, persistence safeguards, absent security/API parity, component-scan layout and legacy exception disclosure.
- Recommended foundation readiness and isolated compatibility fixtures as the next slice. No source, build configuration, skills, API contract or database schema was changed; no module marked COMPLETE. Await approval before the next slice.

---
## 2026-09-16 — Project Conventions Integrated

- Added `PROJECT_CONVENTIONS.md` based on the supplied project convention file.
- Preserved its Entity/DTO/Controller/Service/Repository/REST/validation/error-handling rules.
- Changed the BaseEntity example to UUID + ZPantry audit/soft-delete fields.
- Added migration compatibility exceptions so REST style rules cannot silently redesign legacy APIs.
- Split behavioral compatibility authority from Java implementation-style authority.
- Added project-documentation checks to the Definition of Done.
- Updated `AGENTS.md` and `README_FIRST.md` so agents read the conventions explicitly.

---

## 2026-09-16 — Context Pack Baseline

- Added agent instruction model separating Java skills from ZPantry-specific context.
- Added project/domain context.
- Added legacy-system summary.
- Added database schema context.
- Added target Java architecture.
- Added integration context.
- Added migration status tracker.
- Added architecture decision log.
- Added known-risk register.
- Added migration-focused test strategy.
- Recorded inspected legacy GitHub baseline.
- Existing `.agents/skills/` and `API_MIGRATION.md` are intentionally not duplicated.

---

## Entry Template

### YYYY-MM-DD — Short Title

- What changed.
- What migration status changed.
- What API/database/security decision changed.
- Tests/verifications performed.
- Remaining blocker, if any.




# 2026-09-19 — Authentication evidence closure

- Traced the pinned legacy register, OTP, login, JWT, refresh and logout implementations end to end.
- Captured redacted runtime contracts on isolated .NET 10 and PostgreSQL 16 with synthetic actors and
  intercepted email delivery; no real database, email or secret was used.
- Proved refresh rotation, exact state failures, email-before-insert ordering, empty registration,
  process-dev JTI rejection and loss of revocation after process restart.
- Added `AUTHENTICATION_MIGRATION_ANALYSIS.md`, MIG-005, evidence provenance, mutation/HTTP matrices,
  ownership proposal and revocation alternatives. No Java production/build/security code changed.
# 2026-09-19 — Batch production implementation pass

- Added Java production routes for every public endpoint in the pinned legacy controllers.
- Added Authentication issuance/rotation/OTP/logout with approved input, RNG, ADR-012 and bounded
  dev-revocation corrections.
- Added JPA feature mappings, CRUD/orchestration services, multipart media boundaries, Cloudinary
  adapter, external AI HTTP client and `vector(1536)` persistence.
- Extended the isolated test schema and added initial cross-feature persistence/authentication tests.
- Preserved `ddl-auto=validate`, disabled Flyway execution and environment-only credentials.
- Added a complete endpoint inventory. New modules remain IMPLEMENTED_NOT_VERIFIED pending full
  legacy runtime parity and external integration verification.
# 2026-09-28 — Pantry image import and Recommendation V2 foundation

- Added AI-service Gemini image-analysis boundary; the Java backend remains an orchestrator and
  never stores the provider key.
- Added multipart Pantry Import preview/confirm boundaries and a no-direct-AI-write rule.
- Added controlled profile/allergen enums, V3 allergen metadata columns, and Ingredient/Recipe CRUD
  support for allergen declarations.
- Added Recommendation V2 server-derived Pantry/Profile context and allergen candidate exclusion.
- Verified Java `test` and `clean verify`; diet/goal hard filtering remains pending recipe metadata.
