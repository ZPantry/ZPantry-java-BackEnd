# ZPantry Known Risks and Inconsistencies

This file is intentionally explicit. Do not silently "clean up" these issues during migration.

## R-032 — Batch Feature Parity and Maintainability Remain Open

The batch pass provides every legacy route, but most new feature endpoints lack captured legacy
HTTP fixtures. AI/Cloudinary production connectivity and Today Menu upload compensation are not
verified. Several batch classes are deliberately dense and use map-shaped upstream payloads; they
need type and readability cleanup only after parity is locked. Status: BLOCKS dev COMPLETION.

## R-001 — Legacy Secrets Were Committed

**Severity:** Critical

A legacy authentication configuration file contains values that appear to include real credentials/secrets.

Rules:

- never copy those values into Java;
- rotate affected credentials if still valid;
- use environment variables/secret management;
- deleting the current file alone does not remove values from Git history.

---

## R-002 — Password Hash Compatibility

**Severity:** High

The legacy backend uses `Microsoft.AspNet.Identity.PasswordHasher`.

Spring Security BCrypt is not automatically compatible with those stored hashes.

Impact:

- migrating login incorrectly can lock out all existing users.

Required:

- verify exact legacy encoding algorithm/format;
- add compatibility tests with safe representative hashes;
- define rehash/reset strategy.

---

## R-003 — In-Memory Access-Token Blacklist

**Severity:** High for multi-instance/restart behavior

Legacy logout JTI revocation is stored in process memory.

Impact:

- restart clears blacklist;
- multiple replicas do not share blacklist state.

Migration rule:

- preserving this temporarily is compatibility;
- replacing it with Redis/database-backed revocation is a redesign/security improvement and should be an explicit ADR.

---

## R-004 — Ingredient Documentation vs Executable Code Drift

**Severity:** Medium

Legacy changelog documentation states Ingredient create/update/delete APIs were removed, while inspected executable interfaces/controllers still expose CRUD behavior.

Migration rule:

- executable code and observed API behavior win;
- do not implement the changelog statement without runtime verification.

---

## R-005 — Database Documentation/Entity Drift

**Severity:** High

Ingredient and Recipe entity/DTO code includes gradient fields, while the inspected lightweight skeleton SQL does not include them.

Required:

- inspect actual PostgreSQL schema;
- inspect EF model configuration;
- decide mapping based on real persistence behavior.

---

## R-006 — Lightweight SQL Is Not Sufficient as Sole Schema Source

**Severity:** High

The skeleton SQL defines tables but may not capture final FK/index/constraint behavior.

Required:

- query the actual database;
- inspect EF Core mappings/migrations;
- do not infer constraints merely from `*_id` column names.

---

## R-007 — Vector Search Service Is Stubbed

**Severity:** Medium

The legacy vector-search abstraction currently returns empty lists.

Impact:

- pgvector presence does not mean vector retrieval is a completed feature.

Migration rule:

- preserve existing behavior first;
- real vector search should be tracked as enhancement work.

---

## R-008 — Broad CORS

**Severity:** Medium

Legacy host uses an AllowAll-style CORS policy.

Migration rule:

- do not accidentally block existing clients during parity migration;
- tightening CORS is desirable but should be configured intentionally per environment.

---

## R-009 — Authorization Coverage Needs Explicit Review

**Severity:** High

Some legacy feature controllers appear to have weaker/no controller-level authorization compared with user-owned controllers such as pantry/today-menu/recommendations.

Migration rule:

- do not accidentally expose more than legacy;
- do not silently tighten routes without checking client expectations;
- create an explicit security-hardening decision if changing behavior.

---

## R-010 — AI Service Failure Semantics

**Severity:** Medium

Legacy AI client handles HTTP/timeouts/JSON failures.

Migration risk:

- returning `200` with empty data when the AI service failed can change semantics and hide operational failures.

Required:

- parity-test failure cases;
- map errors intentionally.

---

## R-011 — Migration Coupling Risk

**Severity:** High

Changing language, API contract, database schema, auth semantics, and deployment topology simultaneously makes failures hard to diagnose.

Migration rule:

- use vertical slices;
- keep schema/API changes minimal during initial Java cutover;
- separate modernization work from compatibility migration.

---

## Baseline audit additions — 2026-09-16

These findings describe the inspected Java scaffold. No application/database behavior was changed to resolve them.

## R-012 — API Migration File Reconciled; Historical Example Drift

**Severity:** High

**Current status (2026-09-18):** Resolved documentation blocker. The existing file is present and readable; examples are labeled illustrative and MIG-004 records User analysis only. The following describes the historical audit state, not the current file state.

At the baseline audit, API_MIGRATION.md was absent from the working tree but present in the Git index (AD). Its staged contents are a template and sample MIG-001/002/003 entries, including a status index claiming sample user/product migrations. There are no Java controllers. The sampled /api/v1 routes and numeric IDs are not evidence of the legacy contract: inspected UsersController uses /api/users and GUIDs.

Preserve the user's pre-existing index/deletion state during this audit. Reconcile the existing file before API work; do not create a competing specification or count examples as completed migration. Java currently exposes no implemented business contract.

## R-013 — Foundation Cannot Load Its Application Context

**Current status:** Resolved for the isolated foundation on 2026-09-16: all 3 application-context/persistence ITs pass. The following describes the historical baseline failure.

**Severity:** High (build/startup blocker)

Both clean verify and test fail: 1 test, 1 context initialization error. The datasource cannot determine a suitable driver class because no datasource URL/test database is supplied; the PostgreSQL driver itself is present. The failure propagates through Flyway and entityManagerFactory. Compilation and packaging with tests skipped succeed, but neither verifies startup. No test resources, isolated PostgreSQL fixtures or behavioral tests exist.

Next foundation work must configure an isolated test environment and externalized runtime settings. Disabling persistence or skipping tests must not be reported as migration verification.

## R-014 — Runtime JDK Differs from the Target

**Severity:** Medium

pom.xml targets Java 21, but the working wrapper uses JDK 23.0.2 and PATH java reports Java 8u202. No Maven toolchain or Enforcer rule pins the runtime. Compiler release 21 succeeded; Java 21 runtime execution has not been checked. Establish consistent JDK 21 build/test/deployment execution before claiming the runtime baseline is verified.

## R-015 — Database Safeguards and Vector Mapping Not Established

**Severity:** High

No datasource configuration, ddl-auto=validate, Flyway baseline policy, migration scripts or explicit startup execution guard exists. This does not establish that Hibernate currently issues destructive DDL; startup fails before database access. Supplying a legacy database URL later would activate unverified persistence/migration behavior.

Spring AI pgvector starter 2.0.1 is installed, but no legacy entity embedding mapping, configured dimension or EmbeddingModel bean exists. Its vector-store abstraction must not be assumed to map ingredient/recipe vector(1536) columns. Further auto-configuration failures may appear after the datasource problem is fixed; these have not yet been observed.

Inspect an isolated PostgreSQL 16 schema, establish explicit non-destructive policies, and verify whether this dependency is appropriate before any legacy database connection. Preserve EF history separately from Flyway history. Actual server/extension state, constraints and gradient columns remain unverified.

## R-016 — Security and API Compatibility Are Entirely Unimplemented

**Severity:** High (cutover blocker)

The security starter is present, but no JWT/authentication/authorization configuration, legacy hash verifier, role/owner checks, refresh handling, OTP or blacklist exists. There are no response/error wrappers, pagination adapters or endpoints. Framework defaults are not legacy parity; runtime HTTP security behavior is not verified because startup fails.

Do not deploy this scaffold as a replacement backend. No new password scheme or API contract has been selected. Existing password continuity and secret rotation risks remain unresolved.

The current .gitignore does not protect .env variants or private application configuration. No secrets were found in the inspected Java source/configuration, but future dev credentials could be accidentally staged. Introduce suitable exclusions and secret-free examples when configuration is added; do not copy legacy credentials.

## R-017 — Target Package Layout Falls Outside Current Default Scan

**Severity:** Medium (future implementation risk)

The entry point is in com.zpantry.z_pantry_backend. docs/ARCHITECTURE.md recommends feature packages directly under com.zpantry. Such sibling packages would fall outside the entry point's default scan. No current bean is affected because no feature beans exist. Align the composition root and feature package placement deliberately before implementing modules; do not scatter scan overrides without a documented architecture decision.

## R-018 — Legacy Exception Disclosure Conflicts with Project Conventions

**Severity:** High

Read-only inspection of AuthController at a010fdc5894176596bb195e4fef66db2c09496f1 confirms several catch blocks return ex.Message in HTTP 500 ApiResponse wrappers. PROJECT_CONVENTIONS.md prohibits exposing internal exception details. Java has no handler or selected behavior yet.

Capture representative legacy error responses and explicitly decide which messages can safely remain compatible. Sanitizing observable responses requires a documented compatibility decision rather than silently adopting generic Spring errors; do not reproduce credential disclosure.

## Foundation follow-up — 2026-09-16 (supersedes baseline-only details above)

- R-013: The empty smoke test was replaced with a container-backed application-context IT.
  Unit checks now pass. Integration startup is still blocked, now specifically by Docker
  discovery rather than missing datasource configuration (see R-019).
- R-014: Mitigated in the repository by Java 21 Enforcer/release configuration and actual
  JDK 21.0.12 verification. Global PATH/JAVA_HOME remain machine-owned; IDE/shell users must
  select 21. Java 23 rejection was verified.
- R-015: Safe defaults now explicitly validate, disable SQL initialization/Flyway execution,
  disable clean and automatic baseline. The generic Spring AI starter was removed. Actual
  legacy schema and future production vector mapping are still unverified. Deployment
  overrides must not relax this safety policy.
- R-016: Source-derived response records and explicit Bean Validation now exist, but all
  business security/API behavior remains unimplemented. .env/application-dev exclusions
  now reduce accidental credential staging; secret rotation risks remain open.
- R-017: Resolved by moving the composition root to com.zpantry before feature packages exist.
- R-012 and R-018: Unresolved. The existing API file was not modified because no API migration
  status changed. No blanket error policy was invented; exception/tracing behavior awaits fixtures.

## R-019 — Docker Unavailable; Persistence Tests Not Runtime-Verified

**Current status:** Resolved on 2026-09-16. Docker Npipe discovery, pinned PostgreSQL 16.14 container startup and pgvector 0.8.2 checks passed. Full verify: 4 unit + 3 integration tests, no failures/errors/skips. The following describes the earlier blocker.

**Severity:** High (foundation verification blocker)

Testcontainers 2.0.5 fails with `Could not find a valid Docker environment` on this host.
Docker is absent from PATH and the usual Docker Desktop location. The IT suite compiles,
but its three methods cannot load the context; none is skipped. No database assertion or
container image execution has passed. The image's manifest digest was resolved from the
registry, but image startup and extension/JPA behavior require a working daemon.

Provide a Docker-compatible Linux-container daemon, keep JAVA_HOME on 21, and rerun Maven
verify. Do not bypass this with H2, a developer database, skipping ITs, or marking Foundation
COMPLETE. Runtime mapping issues may still surface once Docker is available.

## Foundation verification and API file investigation — 2026-09-16

R-013/R-019 are resolved for the infrastructure slice. Hibernate validate and disabled
Flyway/SQL-init safety remain intact. Container URL identity, UUID and PostgreSQL mappings,
vector(1536), invalid dimension rejection and absence of Flyway history passed. No source
or configuration fix was needed. Testcontainers cleanup was confirmed after the run.

R-012 remains a documentation reconciliation issue, but the contents are not lost:
D:/zpantry/z_pantry_backend/API_MIGRATION.md is absent on disk and staged as a new file
with an unstaged deletion (AD), not an untracked/ignored file. Index stage 0 blob
53435ba26877d84d6a3120c42c81a5f9040322db is 6300 bytes. The existing Downloads copy at
C:/Users/ASUS/Downloads/API_MIGRATION.md is byte-identical (same unfiltered Git object hash).
No other filename match was found under D:/zpantry; user Documents/Downloads/Desktop were
also searched. Git lists one worktree on unborn master, no commits/branches/stashes.
Accidental deletion versus intentional removal cannot be inferred. No file was restored,
replaced or generated, and the Git index remains unchanged. The template examples still
must not be treated as verified endpoint migrations.

Remaining legacy schema, authentication, endpoint contract and exception disclosure risks
are unchanged. Foundation completion does not certify business modules or production cutover.


## User analysis findings — 2026-09-18

This section supersedes earlier statements that R-012 is unresolved. The existing migration
file is present/readable and now separates examples from factual User analysis (MIG-004).
Foundation remains COMPLETE and unchanged. User remains unimplemented.

## R-020 — Conflicting Users Schema; Actual Catalog Unavailable

**Severity:** High (persistence compatibility blocker)

**Evidence-closure status (2026-09-18): BLOCKED.** dev PostgreSQL 17 required a password;
no connection environment variable or pgpass file was available. A no-prompt connection failed
before a session, so no read-only catalog query ran and no database was modified. The service
was not assumed to be the ZPantry database. An explicit read-only connection/catalog export is required.

**Disposable-runtime update:** PARTIALLY_RESOLVED for implementation. The pinned migration on
PostgreSQL 16.14 produced the exact 20-column snake_case schema, defaults, PK and unconditional
email unique constraint documented from EF/skeleton. The real shared database remains unverified,
so Db.sql drift remains a production-cutover risk.

EF/skeleton maps 20 snake_case columns and application-generated UUIDs. Database/Db.sql
instead uses quoted PascalCase columns, a database UUID default and omits soft-delete/audit
columns and avatar_url. InitialCreate executes CREATE TABLE IF NOT EXISTS and cannot repair
an existing mismatched table. No actual legacy dump/catalog evidence was available here.

Follow the executable mapping as a provisional proposal only; obtain read-only catalog
metadata before finalizing it. Confirm schema/search_path, constraints, unconditional email
uniqueness, collation, defaults, triggers/RLS and timestamps. Deleted emails remain reserved
in the source schema despite registration lookup excluding deleted users. Do not apply old
scripts to a real database or claim the Foundation probe validates users. See DATABASE_SCHEMA.md.

## R-021 — User PUT Requires Legacy-Compatible Password Generation

**Severity:** High (password-changing PUT blocker; extends R-002)

**Evidence-closure status (2026-09-18): RESOLVED for format/strategy; implementation pending.**
The actual 2.2.4 package and independent JDK 21 derivation verified marker 0, PBKDF2-HMAC-SHA1,
1,000 iterations, 16-byte random salt, 32-byte subkey and 49-byte Base64 payload. A synthetic
fixture is stored under contracts/auth. Strategy A (temporary exact legacy generation/verification)
is recommended for coexistence. Java production code and fresh-hash C# cross-tests do not yet exist.

UserService uses Microsoft.AspNet.Identity.Core 2.2.4 PasswordHasher for updates as well as
registration/login. A Java verifier alone cannot guarantee that Java-written hashes remain
usable by the legacy backend. Exact encoding interoperability has not been independently
verified. Prove synthetic Java-generated hashes against the pinned legacy verifier before
writing passwords. No BCrypt substitution, forced reset or password-policy redesign is approved.

## R-022 — User Authorization Depends on Existing Authentication Semantics

**Severity:** High (protected endpoint/cutover blocker)

**Evidence-closure status (2026-09-18): PARTIALLY_RESOLVED.** Literal claim URIs, emitted values,
GUID parsing, owner comparison and admin route behavior are source-verified. Java has no
SecurityFilterChain, JWT decoder/converter, owner identity adapter or blacklist integration, so
it cannot consume an already-valid legacy identity yet. Cross-process revocation remains unresolved.

**Disposable-runtime update:** the actual login flow authenticates tokens for admin endpoints,
but matching-owner PUT still returns the explicit owner 403. A separately signed test token with
the expected claim also failed. This is now a confirmed source/runtime conflict, not merely absent
evidence. Preserve-always-403 versus repair-owner-consumption requires explicit approval.

NameIdentifier contains the UUID; sub contains email. Admin grants list/detail/delete but
never bypasses owner-only PUT. Non-admin own-detail reads are forbidden. A mock principal
cannot prove legacy JWT validation/claim/revocation interoperability. The existing process-dev
JTI blacklist also cannot automatically be shared across Java and C# processes.

Legacy token validation does not reload active/confirmed/deleted flags. Password updates and
soft deletion do not revoke existing access tokens. Record this risk; changing it requires an
explicit decision rather than silently strengthening or weakening the migration contract.
The legacy-system narrative describes issuer/audience validation broadly; Program.cs actually
enables each only when configured nonblank. The source-derived proposal follows that conditional
behavior. No registration/login/JWT issuance/refresh/logout implementation is authorized here.

## R-023 — Framework Error and Boundary Parity Not Captured

**Severity:** High (API parity blocker)

**Evidence-closure status (2026-09-18): BLOCKED.** Only .NET SDK 9 is installed while the source
targets net10.0; Docker was unavailable, and no isolated legacy database/configuration exists.
The host's startup can migrate/create/drop schema, so it was not launched against an unknown
database. No HTTP fixture was fabricated. Runtime capture remains required before parity claims.

**Disposable-runtime update:** PARTIALLY_RESOLVED. Docker supplied .NET 10 and an isolated
PostgreSQL database; 23 raw fixtures now cover list/detail/delete/auth and pagination. Successful,
missing-user and mutation PUT responses remain unreachable because owner authorization always
rejects tested tokens. Framework behavior outside the captured matrix remains unverified.

Source proves missing detail/delete return 200 failure wrappers, missing owner PUT returns
400, and owner rejection returns a Vietnamese 403 wrapper with a request trace ID. Other
service wrappers have empty trace IDs. Generic 404/409, blanket tracing, @NotBlank/@Size,
PATCH or /api/v1 defaults would alter compatibility. These convention examples are not mandates
for migrated routes. API_MIGRATION.md MIG-004 records the source contract.

No captured payloads establish malformed GUID/JSON/query behavior, automatic validation,
bearer challenge bodies, unexpected DB errors, timestamp formatting or unknown-field coercion.
Huge Int32 page offsets and .NET-versus-Java Unicode whitespace also need characterization.
Capture these in isolated legacy/Java environments; do not fabricate responses or expose
internal exception details. User analysis/test gates are in USER_MIGRATION_ANALYSIS.md.

## R-024 — Active JWT Handler Creates Duplicate NameIdentifier Claims

**Status (2026-09-19): PROVEN; migration decision pending.** With JwtBearer 10.0.9 and resolved
IdentityModel 8.0.1, the active validator is `JsonWebTokenHandler`, not
`JwtSecurityTokenHandler`. `JwtBearerOptions.MapInboundClaims` and the active handler's mapping
are true; `JsonWebTokenHandler.DefaultInboundClaimTypeMap` has 73 entries. Legacy startup clears
only `JwtSecurityTokenHandler` maps.

Raw email-valued `sub` is mapped to the NameIdentifier URI before the token's existing
UUID-valued NameIdentifier. `UsersController.FindFirstValue(ClaimTypes.NameIdentifier)` selects
the email, UUID parsing fails and every tested login-issued owner PUT returns explicit 403. An
equivalent separately signed token with `sub` plus UUID `nameid` fails the same way. Redacted
runtime evidence is in `contracts/users/evidence/claims-diagnosis-2026-09-19`.

Preserving 403 gives strict observed compatibility but retains broken self-update. Correct UUID
extraction is an intentional deviation and requires approval of proposed ADR-012. Java must not
accept arbitrary claims: extraction happens only after signature, configured issuer/audience,
lifetime and JTI validation.

## R-025 — Shared-Database Cutover Evidence Still Missing

**Status (2026-09-19): implementation limitation; cutover blocker.** Pinned migrations and the
disposable PostgreSQL catalog provide enough evidence for dev User implementation. They do not
prove the actual shared table's constraints, indexes, collation, triggers/RLS/grants, migration
history, production data quality or security-field variants. User implementation is READY WITH
DOCUMENTED LIMITATIONS; production cutover remains BLOCKED until read-only catalog comparison.

## User implementation risk update — 2026-09-19

R-024 is resolved for Java: accepted ADR-012 prevents email `sub` from shadowing UUID ownership,
and regression tests cover the captured duplicate-claim shape. The intentional difference from
legacy remains documented in MIG-004.

R-003 remains open for production: `TokenRevocationChecker` is only a boundary and tests use a
deterministic fake. JWT-enabled runtime configuration requires a real checker bean, so the app
does not silently claim revocation parity. R-025 remains the production database cutover blocker.
## R-026 — Legacy Registration Accepts Empty Identity and Password

Runtime capture proves `POST /api/Auth/register` with `{}` succeeds, persists blank email/name and
a hash of the blank password. The DTO has no validation annotations and missing strings default to
empty. Adding ordinary Java validation would change observed behavior; preserving it creates an
account integrity and security risk. Authentication implementation requires an explicit compatibility
decision. Status: OPEN.

## R-027 — OTP Generation Is Predictable and Has No Retry Enforcement

Legacy registration uses `Random.Shared` for a six-digit OTP and verification never increments or
checks `otp_retry_count`. Wrong and expired attempts do not mutate the row, and there is no resend
route. A secure Java generator and attempt policy would intentionally deviate from legacy behavior.
Status: OPEN; decision required before register/OTP implementation.

## R-028 — Logout Revocation Is Process-dev and Lost on Restart

The C# blacklist is a static `ConcurrentDictionary` keyed by JTI. Runtime evidence proves logout
rejects the token in-process, but restarting the process makes the same still-valid token usable.
Java and C# cannot observe each other's revocations, and multiple C# instances disagree. Mixed
runtime rollout is blocked until a shared store is approved or routing guarantees one auth-owning
runtime plus a maximum-token-lifetime drain. Status: BLOCKING PRODUCTION CUTOVER.

## R-029 — Registration Email Side Effect Precedes Persistence

Resend delivery occurs before user insertion. Provider failure produces HTTP 500 and no row, while
successful delivery followed by database failure can send an OTP that has no account. Error details
are exposed through the controller's 500 envelope. Status: OPEN compatibility/security decision.
## R-030 — Batch Feature Contracts Are Not Yet Runtime-Captured

Ingredient, Recipe, Media, Pantry, Recommendation and Today Menu source methods have Java
implementations, but their HTTP response/status parity has not been captured against the legacy
runtime. AI and Cloudinary production connectivity are also unverified. Modules remain
IMPLEMENTED_NOT_VERIFIED. Status: OPEN and blocks dev completion.

## R-031 — Today Menu External Upload Cannot Roll Back Atomically

Legacy completion deletes an uploaded Cloudinary asset after a database transaction failure. The
initial Java implementation performs the database work transactionally but does not yet provide
equivalent compensating deletion for every post-upload failure. Status: OPEN.

## R-033 — Recommendation Persistence and Response Contract Diverge From Legacy

The current Java recommendation orchestration returns the AI adapter's raw response map as the
`data` member, persists only the `meal_recommendations` parent row, and does not persist ranked
`meal_recommendation_items`. The legacy service unwraps and maps the AI payload, provides a dev
fallback for meal recommendation/check operations, completes the parent state, and stores every
result item. Stored-detail and feedback response shapes also differ. Status: CONFIRMED BUG; blocks
dev completion for all five Recommendation endpoints.

## R-034 — Today Menu Detail, Log Expansion, and Compensation Are Incomplete

The Java detail response currently returns only the base menu item, while legacy includes recipe,
required ingredients, current pantry items and usage logs. Java cooking-log listing emits empty
usage-log arrays. Completion has a database transaction but does not compensate the uploaded media
asset when a post-upload database operation fails. Status: CONFIRMED BUG; blocks dev completion
for detail, completion and cooking-log endpoints.

## R-035 — AI Client Failure Contract Is Not Yet Bounded

The Java HTTP adapter wraps `RestClientException`, but no configured connect/read timeout or typed
validation exists. Empty, malformed and structurally incomplete responses are not distinguished,
and Recommendation currently exposes adapter exceptions through generic application handling.
Pantry text, receipt, and food-image flows now have a bounded catalog-preview contract: malformed
text-model schemas produce a safe 503 response, while unresolvable image detections are omitted
instead of emitting fabricated IDs. Recommendation and other AI clients still lack the same typed
timeout/response contract. Production AI connectivity remains a separate deployment check.

## R-036 — Shared Catalog May Differ From the Java V1 Baseline

V1 follows the EF skeleton, DbContext mappings and disposable evidence, but the actual shared
catalog remains unavailable. Legacy evidence has no foreign keys and no indexes beyond unique user
email. Production adoption must compare constraints, indexes, defaults, extensions, grants/RLS,
triggers and data quality before assigning a Flyway baseline. Automatic baselining remains disabled.
Status: BLOCKING PRODUCTION DATABASE ADOPTION.
## R-035 — Diet and goal recipe suitability metadata is absent

Recommendation V2 can hard-filter declared recipe allergens. `UserGoal` and `DietPreference` are
controlled profile enums but recipes do not yet declare diet suitability or nutrition thresholds, so
these values cannot safely be hard-filtered. Do not claim diet/goal compliance until that metadata
and tests exist.
