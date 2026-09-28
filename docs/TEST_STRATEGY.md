# ZPantry Migration Test Strategy

## Database migration and seed verification

Persistence integration tests start with an empty PostgreSQL 16 + pgvector Testcontainer. Flyway
applies production V1 and Hibernate validates it. `FoundationPersistenceIT` checks the catalog,
critical types, vector dimensions and second-run idempotency. `SeedDataIT` explicitly applies the
test seed and verifies users, roles, aliases, recipe links, cross-user pantry rows, Today Menu and
synthetic media data.

## 1. Goal

Tests must answer a migration-specific question:

> Does the Java implementation behave like the legacy backend for the scenarios clients and data depend on?

Unit tests alone cannot prove that.

---

## 2. Test Layers

### Unit Tests

Use for:

- pure business rules;
- mapping logic;
- token helper logic;
- validation;
- normalization;
- pantry quantity calculations;
- status transitions.

Use the existing Java testing skills for style and tooling.

### Spring Integration Tests

Use for:

- controller + security behavior;
- request validation;
- response wrapper;
- authorization;
- repository interactions;
- transaction behavior.

### PostgreSQL Integration Tests

Prefer a real PostgreSQL test instance/Testcontainers for:

- UUID mapping;
- snake_case mapping;
- timestamp behavior;
- numeric precision;
- soft deletion;
- pgvector;
- repository queries.

Do not rely on H2 to prove PostgreSQL-specific compatibility.

### External Client Tests

Mock/stub AI and Cloudinary boundaries for deterministic tests.

Verify:

- successful response mapping;
- timeout;
- connection failure;
- non-2xx;
- malformed response;
- missing fields.

---

## 3. Legacy-vs-Java Parity Tests

For each migrated endpoint, capture representative legacy behavior.

Compare at least:

- route;
- method;
- required headers/auth;
- request fields;
- status code;
- response body shape;
- error body shape;
- data side effects.

Recommended process:

```text
same fixture
   ├──► legacy backend ──► capture response + DB effect
   └──► Java backend   ──► capture response + DB effect

compare
```

Do not point both implementations at the same writable database during destructive parity tests.

Use isolated clones/containers or fixture resets.

---

## 4. Authentication Compatibility Tests

High priority cases:

- register;
- verify OTP;
- login with newly created Java password;
- login with representative legacy password hash;
- wrong password;
- inactive/unverified user behavior;
- refresh token success;
- expired refresh token;
- refresh rotation;
- logout;
- revoked JTI;
- role-protected endpoint;
- owner-only endpoint.

Do not mark Auth complete until legacy password continuity is intentionally resolved.

### Captured Authentication contract

The redacted fixture set at `src/test/resources/contracts/auth/evidence/2026-09-19/` comes from
commit `a010fdc5894176596bb195e4fef66db2c09496f1` on disposable .NET 10 and PostgreSQL 16. It covers
register, OTP, login, refresh rotation/state failures, logout and loss of revocation on restart.
Future Java parity tests must compare status, envelope/message, JSON field presence/casing and
database mutations. Normalize UUIDs, timestamps, trace IDs, OTPs and token values; validate token
structure/claims/lifetimes separately. Never update a fixture merely to match Java output—re-capture
or document an approved deviation. Passwords and raw tokens must remain memory-only in test code.

### Batch implementation verification

`RemainingFeaturesIT` uses the isolated PostgreSQL 16/pgvector environment and in-process fake AI,
media and email ports. It verifies Authentication registration/OTP/login/refresh rotation and
Ingredient/Recipe/Pantry persistence including 1536-dimensional embeddings. This is initial system
integration coverage. It does not replace pending controller parity, upstream timeout/malformed
response cases, multipart response contracts, or Today Menu compensation/concurrency tests.

---

## 5. Persistence Compatibility Tests

Test:

- soft-deleted rows excluded;
- UUID identity;
- pagination ordering;
- decimal quantities/nutrition precision;
- date/timestamp serialization;
- recipe-ingredient relationships;
- pantry user scoping;
- today-menu planned date;
- cooking log + pantry usage side effects.

---

## 6. Recommendation/AI Tests

Test the orchestration, not AI intelligence.

Verify that the Java backend:

- constructs expected AI request DTO;
- handles AI response;
- persists recommendation/session items appropriately;
- preserves ranking/reason/missing-ingredient data;
- scopes records to the authenticated user;
- maps failures correctly.

---

## 7. Definition of Parity

A module may move to `COMPLETE` when:

- important happy paths pass;
- important error paths pass;
- auth/ownership behavior is verified;
- persistence side effects are verified;
- API response compatibility is verified;
- known intentional differences are documented in an ADR or API migration record.

## User evidence fixtures

`src/test/resources/contracts/users/evidence/` is reserved for unmodified legacy User HTTP
captures and provenance manifests. A README currently records why capture was blocked; the
absence of JSON is intentional. Never substitute source-derived examples for observed payloads.
Future captures must include request, status, relevant headers, exact body and before/after
database effects, using an isolated legacy database. Dynamic-field comparison rules belong in
a separate manifest and must not erase meaningful empty/null trace or timestamp behavior.

`src/test/resources/contracts/auth/password-hash-v0-vector.properties` is a safe synthetic
cross-runtime vector generated by Microsoft.AspNet.Identity.Core 2.2.4 and verified independently
on JDK 21. It contains no production credential. Future Java tests must use it as a fixed
regression vector and also verify generated hashes with fresh random salts against the actual
legacy verifier. This closes algorithm/format evidence, not User endpoint parity.

### Captured User evidence — 2026-09-18

The captured directory contains 23 real cases from the pinned .NET 10 runtime, raw Kestrel
bodies/headers, a provenance manifest, before/after rows and the disposable users catalog.
Authentication tokens came from legacy login and were not persisted. The database was
PostgreSQL 16/pgvector on a private Docker network with synthetic data only.

Covered: admin list/default/explicit/boundary pagination, list/detail authentication and role
failures, existing/missing/deleted detail, owner/admin/anonymous PUT attempts, admin/non-admin/
anonymous delete, repeated delete and exact soft-delete effects. Empty middleware response bodies
are meaningful fixtures. Tests comparing Java later must treat fixture timestamps, trace IDs,
UUIDs and Date headers as declared dynamic fields without rewriting source evidence.

No successful PUT fixture exists. The legacy login token is accepted for `[Authorize]`, but
matching-owner PUT still reaches the explicit owner 403. All attempted PUT before/after rows were
identical. This is a compatibility decision blocker, not a missing snapshot to approve away.

## Executable foundation checks — 2026-09-16

Prerequisites: JDK 21 and a working Docker-compatible Linux-container daemon for integration
tests. Set JAVA_HOME to the JDK 21 installation and put its bin directory first on PATH in
your shell/IDE. The wrapper does not install or choose a JDK. Maven rejects Java 8/17/23+.
On this audit host JDK 21 is available at C:/Users/ASUS/.jdks/ms-21.0.12; that is a dev
observation, not a required portable path.

Run from the repository root:

```text
./mvnw -version
./mvnw clean test
./mvnw verify
```

On Windows use .\mvnw.cmd. clean test runs the unit/serialization/configuration/validation
checks. verify additionally runs FoundationPersistenceIT using Failsafe. Do not use only
integration-test: verify is needed to propagate integration failures. Surefire/Failsafe
reports are in target/surefire-reports and target/failsafe-reports.

The former empty contextLoads smoke test is replaced by a real application-context
integration test importing a container-owned DataSource. Test initialization creates a
fresh PostgreSQL 16 + pgvector 0.8.2 database using a manifest-digest-pinned image. No fixed
host port, bind-mounted database, shared database, reuse, external URL fallback or Docker
absence skip exists. Test writes are transactional and rolled back. Container shutdown
removes its disposable resources. Unit tests never start a datasource.

Database assertions cover the exact container URL, PostgreSQL major version, vector
extension version, absence of Flyway history, UUID/numeric/date/Instant JPA round-trips,
and acceptance of 1536-dimensional vectors with rejection of incorrect dimensions.
The vector test uses JDBC, not an implemented production vector entity mapping.

Verification on 2026-09-16 passed with Docker available: 4 unit and 3 integration tests,
zero failures/errors/skips, JDK 21.0.12, PostgreSQL 16.14 and pgvector 0.8.2. The container-only
URL assertion and all persistence/vector checks passed. Cleanup left no Testcontainers-labelled
containers. This establishes foundation readiness, not legacy endpoint/table parity.

### Legacy fixture strategy

See src/test/resources/contracts/README.md and feature directories for auth, users,
ingredients, recipes, pantry, recommendations and today-menu. These are test evidence
containers, not another API specification. They currently contain documentation only.
Each future fixture must identify its pinned source/OpenAPI/capture provenance, HTTP
status/headers, identity context, sanitized setup and DB side effects. Compare parsed JSON
strictly (null versus missing, arrays, fields and types); normalize only explicitly listed
nondeterministic values, while validating timestamp and trace semantics independently.
No credentials or personal data may enter fixtures. A changed expected result needs legacy
evidence or an approved API migration entry, not a snapshot refresh to silence a failure.

### User ClaimsPrincipal diagnostics — 2026-09-19

`contracts/users/evidence/claims-diagnosis-2026-09-19/` contains redacted raw-JWT and
post-validation principal representations for a real login token and an equivalent separately
signed token. It also contains exact PUT request metadata and raw 403 response headers/bodies.
Bearer/refresh tokens, signing key and raw JTI values are not persisted. Diagnostic code existed
only in the disposable legacy copy.

If Option B is approved, Java security tests must prove that signature, configured issuer and
audience, lifetime and JTI validation happen before conversion; `userId` UUID wins over email
`sub`; UUID-valued NameIdentifier is an allowed fallback; duplicate ordering cannot select email;
missing/malformed/conflicting identity is rejected; and roles come only from validated claims.
Controller parity tests must label legacy owner 403 as observed-defect evidence while asserting
approved Java owner success, wrong-owner 403 and admin-other 403.

### Implemented User checks — 2026-09-19

Unit tests verify the Microsoft Identity v2 vector, wrong-password rejection, marker/length,
random salt and Java round-trip hashing. Claim tests verify `userId` precedence over mapped email
NameIdentifier, unambiguous UUID fallback, malformed/missing rejection and role normalization.

`UserSliceIT` uses the existing isolated PostgreSQL 16/pgvector container. It validates all 20
columns, Hibernate validation, UUID/timestamp/null mappings, email ordering, page normalization,
soft-delete filtering and mutation boundaries. MockMvc checks admin/owner/anonymous authorization,
legacy empty middleware bodies, missing-resource wrappers, Option-B PUT, sensitive-field omission,
and a real signed HS256 token through the configured decoder. No H2 or external datasource is used.

## Pantry Import and Recommendation V2

Test image size/type rejection, AI timeout/malformed responses, and the invariant that analysis
does not mutate Pantry. Test confirm ownership and atomic multi-item writes. Recommendation V2 tests
must prove server-side Pantry/Profile use, exclusion of recipes whose allergen set intersects the
user allergy set, and no cross-user context access. AI service tests use deterministic fakes; never
require a real Gemini key.
