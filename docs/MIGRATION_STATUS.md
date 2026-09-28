# ZPantry Java Migration Status

Last update: 2026-09-19 — Authentication contract captured; implementation not started

## Current conclusion

Foundation readiness is COMPLETE for the explicitly scoped infrastructure slice: Java 21,
isolated PostgreSQL/pgvector verification, safe persistence defaults, common envelope checks
and contract-fixture structure. The User slice is implemented and verified for dev migration
scope under accepted ADR-012; this is not production cutover completion. Other business modules
remain NOT_STARTED.

## Module matrix

| Module | Current state | Status |
|---|---|---|
| Foundation | Java 21 enforced, safe persistence defaults, response records, validation, contract directories, container integration tests | COMPLETE (Foundation readiness scope only) |
| User | Persistence, DTOs, service, routes, legacy password hashing and Option-B identity boundary implemented and container verified | COMPLETE (dev migration scope); production cutover BLOCKED |
| Authentication | Register/OTP/login/JWT/refresh/logout implemented with approved corrections and dev revocation | IMPLEMENTED_NOT_VERIFIED |
| Ingredient | CRUD, multipart media and embedding persistence implemented | IMPLEMENTED_NOT_VERIFIED |
| Recipe | CRUD, ingredient links, multipart media and embedding persistence implemented | IMPLEMENTED_NOT_VERIFIED |
| Pantry | Authenticated owner-scoped list/upsert/update/delete implemented | IMPLEMENTED_NOT_VERIFIED |
| Recommendation | Routes, persistence boundary and external AI orchestration implemented | IMPLEMENTED_NOT_VERIFIED |
| Today Menu | Owner-scoped menu, completion, cooking/pantry logs implemented | IMPLEMENTED_NOT_VERIFIED |
| Media | Cloudinary port/adapter and upload/delete routes implemented | IMPLEMENTED_NOT_VERIFIED |
| Cooking / Pantry Usage Logs | No business implementation | NOT_STARTED |
| AI Integration / Embedding Backfill | HTTP client and ingredient/recipe embedding persistence implemented; backfill command absent | IMPLEMENTED_NOT_VERIFIED |
| Vector Search | No implementation; legacy service remains a stub | NOT_STARTED |
| Docker / Deployment | Test image configured; no application deployment configuration | NOT_STARTED |

## Foundation inventory

- Boot parent/plugin 4.0.8; Maven wrapper 3.3.4 uses Maven 3.9.16 unchanged.
- Java release 21 explicitly configured. Maven Enforcer 3.6.2 rejects any runtime outside
  [21,22). .java-version records 21; Surefire/Failsafe 3.5.6 inherit the Maven JDK.
- Actual verification ran with Microsoft JDK 21.0.12. The host's default Maven JDK remains
  23.0.2 and PATH java remains 8; no global machine settings changed. Set JAVA_HOME in the
  shell/IDE. Default-JDK execution was checked and correctly rejected by Enforcer.
- Main class moved to com.zpantry to include the documented future feature package layout
  in default component/JPA/repository scanning. No feature packages implemented.
- common.api contains ApiResponse, ApiErrorDetail and PagedResponse records based on pinned
  legacy source. Envelope/flat pagination serialization and boundary semantics have unit
  tests; these do not establish runtime endpoint parity.
- Explicit validation starter and provider test. Global exception handling and trace-ID
  generation were audited and deferred until actual endpoint/error fixtures justify policy;
  traceId remains a response field. No new header contract or blanket error mapping.
- JPA uses validate; SQL initialization never; Flyway disabled, auto-baseline false, clean
  disabled; Open Session in View false. No runtime migration/schema/extension creation.
- Datasource configuration is external only, with no developer/legacy URL default. Private
  .env and application-dev files are ignored. No secrets copied.
- Removed unused Spring AI BOM/vector-store starter. JDBC PostgreSQL driver and Flyway
  dependencies remain; no inference bean or generic vector-store schema is introduced.
- Testcontainers PostgreSQL 2.0.5 is test-only. IsolatedPostgres pins pgvector 0.8.2 / PG16
  by manifest digest, disables reuse, constructs a container-only DataSource and owns its
  lifecycle. Test SQL creates only a disposable probe table and extension; never legacy DDL.
- FoundationPersistenceIT replaces the empty contextLoads test. It checks the application
  context, connection identity, PostgreSQL version, extension, UUID/decimal/date/Instant
  persistence, vector(1536) round-trip and wrong-dimension rejection. Database writes roll
  back; no externally configurable datasource fallback or Docker-missing skip exists.
- src/test/resources/contracts contains feature directories and provenance/comparison
  instructions. No captured endpoint payloads were invented or added.

## Verification evidence

| Command/check | Result |
|---|---|
| Existing scaffold compile before changes | PASS on JDK 21 |
| .\mvnw.cmd -version with JDK 21 JAVA_HOME | Maven 3.9.16, Microsoft Java 21.0.12 |
| .\mvnw.cmd clean test | PASS; 4 tests, 0 failures/errors/skips: response serialization/pagination, persistence defaults and validation |
| .\mvnw.cmd -B -ntp verify | PASS on 2026-09-16 at 14:42:44 +07:00; 4 unit + 3 integration tests, zero failures/errors/skips |
| .\mvnw.cmd validate with default JDK 23 | Expected FAIL: Enforcer requires JDK 21 |

Docker was detected through npipe:////./pipe/docker_engine. The pinned image started
PostgreSQL 16.14 with pgvector 0.8.2. The datasource URL matched the container URL exactly:
jdbc:postgresql://devhost:55655/zpantry_foundation_test?loggerLevel=OFF (ephemeral port).
Connectivity, UUID/numeric/date/Instant JPA round-trips, vector(1536) round-trip and invalid
dimension rejection passed. Hibernate validated the disposable table; Flyway history was
absent. Configuration safety checks passed. No developer/legacy datasource fallback exists
in IsolatedPostgres, and no legacy database was contacted or changed. Docker ps after Maven
exit showed no remaining Testcontainers-labelled containers. No code/configuration changes,
H2 substitutions, test skips or integration-test disabling were needed in this verification.
Reports: target/surefire-reports and target/failsafe-reports.

### Foundation Definition of Done assessment

- Java 21 execution and full build/unit/integration lifecycle: verified.
- Persistence primitives, pgvector success/error paths and isolated writes: verified.
- Schema safety: validate-only, SQL init/Flyway execution disabled; no real DB use.
- Common envelope and pagination source-derived serialization: unit-verified; not endpoint parity.
- Contract fixture structure/provenance guidance: present, no fabricated endpoint payloads.
- Business HTTP behavior, authorization, ownership and actual legacy table compatibility:
  not part of this infrastructure slice; still required for each future business module.
- Documentation and unresolved risks: updated. API migration statuses unchanged.

## Contract and compatibility limits

Legacy source reviewed at a010fdc5894176596bb195e4fef66db2c09496f1 includes ApiResponse,
ApiErrorDetail, PagedResponse and host controller registration. No secret configuration was
fetched. Source-derived camel-case/null/timestamp expectations require endpoint captures
before a business module can be certified compatible.

API_MIGRATION.md is now present and readable at the repository root. The earlier working-tree
absence is resolved; its historical investigation remains in the changelog. No replacement
was generated. MIG-001/002/003 are explicitly illustrative; MIG-004 records source-derived
User findings with PENDING status, not implemented or verified APIs.

User analysis inspected the pinned C# entity, mapping, repository, service, controller, DTOs,
authentication field writes, host configuration and both conflicting SQL definitions. See
[User analysis](USER_MIGRATION_ANALYSIS.md), [mapping](DATABASE_SCHEMA.md) and MIG-004.
No actual legacy catalog/dump or captured User responses were available. No database was
contacted, no Java/Foundation files changed and no tests were rerun for documentation only.
Actual legacy schema, hash continuity, auth, business side effects and live contract parity
remain unverified. The test probe is not a substitute for inspecting the real schema.

## Next action / next slice

User analysis is finished. Resolve actual users schema evidence, runtime contract captures and
existing-token authorization integration before implementation approval. Password encoding
evidence is now resolved below, but its adapter remains unimplemented. Do not start User or
Authentication implementation from this record. Foundation remains COMPLETE within its scope.

## Recommendation V2 preparation — 2026-09-28

Profile recommendation inputs are now controlled Java enums: `UserGoal`, `DietPreference`, and
`FoodAllergen`. Existing columns are retained; enum names are persisted as canonical text values.
The next implementation phase is MIG-007: an authenticated, server-derived recommendation flow
using Pantry/Profile data, deterministic allergy/diet filtering, and AI ranking. It is PLANNED and
not yet endpoint- or integration-verified.

## User evidence-closure result — 2026-09-18

- Actual users table: BLOCKED. dev PostgreSQL requires credentials that are not available;
  no authenticated/read-only session was established and no SQL ran.
- Legacy HTTP captures: BLOCKED. No dev pinned backend/config/isolated database; installed
  SDK is .NET 9 while source targets .NET 10, and Docker was unavailable. No payload fabricated.
- Password generation: RESOLVED. Actual Microsoft.AspNet.Identity.Core 2.2.4 package execution
  and independent JDK 21 verification prove version-0 PBKDF2-HMAC-SHA1, 1,000 iterations,
  16-byte salt, 32-byte subkey and 49-byte Base64 payload. Synthetic fixture added.
- Authorization claims: PARTIALLY_RESOLVED. Literal URI claims, GUID ownership and admin rules
  are source-verified; Java has no JWT consumer/claim converter/revocation integration yet.

Overall User readiness is BLOCKED. Required persistence and runtime contract evidence cannot
be safely deferred. No Java feature/Foundation code changed. JDK 21 compilation passed before
documentation/evidence updates. A final JDK 21 `clean verify` passed all 4 unit tests, then
failed all 3 existing Foundation integration tests at context startup because Testcontainers
could not find the currently stopped/unavailable Docker engine. No test was skipped or changed.
This is an environment regression from the previously recorded passing Foundation verification,
not evidence of a Foundation code/configuration regression, so Foundation status is unchanged.

## Disposable runtime evidence result — 2026-09-18

- Docker/Foundation: VERIFIED again. JDK 21 verify passed 4 unit + 3 integration tests with
  PostgreSQL 16.14/pgvector, no failures/errors/skips and container-only datasource.
- Disposable legacy runtime: VERIFIED. Pinned net10.0 backend, official migration and synthetic
  bootstrap ran on a private PostgreSQL 16/pgvector database with no real/external dependencies.
- Actual users table: BLOCKED. The VERIFIED disposable catalog exactly matches the 20-column
  EF/skeleton mapping and contradicts Db.sql, but the shared legacy catalog remains unavailable.
- HTTP contract: PARTIALLY_RESOLVED. Twenty-three raw cases verify list/detail/delete/auth and
  soft deletion. Source-intended successful PUT is unreachable because matching owner tokens
  receive explicit 403 before the service.
- Password: RESOLVED; runtime bootstrap/login reconfirmed the legacy format. Password PUT could
  not execute because of the authorization conflict.
- JWT identity: PARTIALLY_RESOLVED; emitted claims are known, but pinned runtime owner lookup
  fails despite a matching emitted NameIdentifier. Java identity consumption remains unimplemented.

This 2026-09-18 conclusion was superseded by the claims diagnosis below. Implementation still
requires approval and an Option A/B choice; its technical evidence classification is no longer
BLOCKED. Production cutover remains blocked on the actual users catalog.

## User claims diagnosis — 2026-09-19

- Reproduced owner PUT through real legacy login: HTTP 403 with the controller's owner-only
  Vietnamese response wrapper.
- Raw token contains email `sub`, UUID `userId`, UUID literal NameIdentifier URI, email, JTI,
  plain role and literal role URI.
- Active `JsonWebTokenHandler` 8.0.1 had inbound mapping enabled. It mapped `sub` to a first,
  email-valued NameIdentifier and kept the original UUID-valued claim second. The controller's
  `FindFirstValue` returned email and UUID parsing failed.
- An equivalent separately signed token with email `sub` and UUID `nameid` failed identically.
- Classification: proven legacy claim-mapping/configuration bug. Option B, deterministic UUID
  extraction and source-intended owner authorization, is recommended but awaits approval.
- **Implementation readiness:** READY WITH DOCUMENTED LIMITATIONS based on pinned source,
  migrations, generated disposable catalog and captured runtime behavior.
- **Production cutover readiness:** BLOCKED pending read-only comparison with the actual shared
  users catalog and an interoperable revocation decision.

## User implementation result — 2026-09-19

The package-by-feature User slice now maps the verified 20-column table using UUID/Instant audit
fields and soft deletion. It implements one-based/clamped email-ordered pagination, admin-only
list/detail/delete, owner-only partial PUT, exact response envelopes, legacy missing-resource
statuses and the ASP.NET Identity v2 password format. Entities are never serialized.

ADR-012 Option B is accepted and implemented: validated `userId` UUID has precedence; only an
unambiguous UUID-valued NameIdentifier is a fallback; email `sub` is never ownership identity.
The JWT decoder validates HS256, zero-skew timestamps, optional configured issuer/audience,
nonblank JTI and the injected revocation boundary. User routes fail closed when JWT support is
disabled, and enabling it without a revocation checker fails startup rather than assuming no
tokens are revoked. Authentication flows remain NOT_STARTED.

User is COMPLETE for dev migration scope after Java 21 `clean verify`. Production cutover stays
BLOCKED on the actual shared users catalog and cross-runtime revocation storage.

Verification: Microsoft OpenJDK 21.0.12, Maven `clean verify` BUILD SUCCESS; 8 unit tests and
11 integration tests, with zero failures, errors or skips. Both integration contexts used private
Testcontainers PostgreSQL 16.14 databases initialized from the test-only schema.

## Authentication analysis result — 2026-09-19

Pinned source and an isolated .NET 10/PostgreSQL 16 runtime now establish register, OTP, login,
JWT issuance, refresh rotation and logout contracts. The redacted evidence covers success and
failure states, database mutations, token metadata, provider-failure ordering and restart behavior.
No Java Authentication production code was added.

Authentication implementation readiness is READY WITH DOCUMENTED LIMITATIONS. Register/OTP need
an explicit decision on preserving unsafe empty-input and non-cryptographic OTP behavior. Logout
needs an approved shared-store strategy or a single-runtime routing/drain plan. Production cutover
remains BLOCKED by cross-runtime revocation and actual shared users-catalog verification. Details:
[Authentication analysis](AUTHENTICATION_MIGRATION_ANALYSIS.md) and API_MIGRATION.md MIG-005.

## Batch implementation pass — 2026-09-19

All public methods found in the pinned legacy controllers now have Java routes; see
[endpoint coverage](ENDPOINT_COVERAGE.md). Remaining features are intentionally marked
IMPLEMENTED_NOT_VERIFIED because only startup/schema validation, the prior User suite, and initial
Authentication/Ingredient/Recipe/Pantry persistence tests currently execute. Full response parity,
AI failure modes, media compensation and Today Menu detail/completion coverage remain required
before any new module is COMPLETE FOR dev MIGRATION SCOPE.

Final batch build: Java 21.0.12 `mvnw clean verify` BUILD SUCCESS. Eight unit tests and thirteen
PostgreSQL 16/pgvector integration tests passed with zero failures, errors or skips. The new tests
cover Authentication rotation and Ingredient/Recipe/Pantry persistence. Untested parity areas
above keep the new modules at IMPLEMENTED_NOT_VERIFIED.

## System-wide parity verification in progress — 2026-09-19

The first verification pass found and fixed two evidence-backed Ingredient defects: search was
applied after database pagination (incorrect result pages and totals), and the v2 update route
discarded the uploaded image. Duplicate active normalized names are now rejected on update as in
legacy. Two PostgreSQL/pgvector integration tests cover these regressions.

The same pass confirmed material Recommendation and Today Menu differences recorded as R-033 and
R-034. These modules cannot be promoted by adding superficial route tests. Authentication and User
regression coverage remains green. Ingredient remains IMPLEMENTED_NOT_VERIFIED until all six HTTP
contracts, soft deletion, embedding dimension failures and multipart failure behavior are covered.
No feature status is promoted by this interim pass.

## Clean-database migration result — 2026-09-19

Java now owns a V1 Flyway baseline for empty/new PostgreSQL 16 databases. Testcontainers proves an
empty database can install pgvector, create all 13 mapped tables, pass Hibernate validation, accept
UUID/numeric/timestamptz/vector(1536) values and run Flyway again with zero migrations. A separate
deterministic test seed covers users and core relationships without external services.

Existing legacy database adoption remains BLOCKED. Default Flyway remains disabled and
`baseline-on-migrate=false`; no shared database was contacted or changed.
