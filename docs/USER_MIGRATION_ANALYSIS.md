# User migration analysis

Analysis completed 2026-09-18; claims evidence closed 2026-09-19. Implementation awaits approval. Foundation is unchanged.
User is ANALYZING, not COMPLETE. No Java feature code or captured payloads were added.

## Evidence and authority

The public contract is maintained in [API_MIGRATION.md, MIG-004](../API_MIGRATION.md).
The complete 20-column mapping, defaults and schema conflict are maintained in
[DATABASE_SCHEMA.md](DATABASE_SCHEMA.md#user-slice-mapping-analysis--2026-09-16).
These are source-derived findings, not verified live database/API parity.

Reviewed legacy commit: `a010fdc5894176596bb195e4fef66db2c09496f1`.
Inspected BaseEntity, User, ZpantryDbContext, IUserRepository/UserRepository,
IUserService/UserService, UsersController, UserDtos, AuthController and its request/response
DTOs, TokenBlacklistService, common response types, host Program.cs, project dependencies,
InitialCreate, InitialSkeleton.sql and Database/Db.sql. Other feature source searches found
no additional direct User repository/security-field writes. Secret configuration was not fetched.

No actual legacy schema dump, catalog output, exported OpenAPI or captured User responses
was found in the project evidence. Foundation's disposable probe schema is not evidence of
the live users table. No legacy database was contacted. The migration guide is now present
and readable in the working tree; its earlier missing-file blocker is resolved. Its original
MIG-001/002/003 examples are explicitly labeled illustrative; MIG-004 records analysis only.

## Proposed Java implementation files

All paths below are proposals, not created files. Keep constructor injection and the existing
package-by-feature structure. Apply the repository Java type-design and Spring REST skills
subject to PROJECT_CONVENTIONS.md and the legacy compatibility exceptions.

| Proposed path under src/main/java/com/zpantry | Responsibility |
|---|---|
| common/persistence/BaseEntity.java | Mapped superclass for UUID and seven audit/soft-delete fields; no blanket update timestamp hook |
| user/entity/User.java | Class extending BaseEntity; explicit existing columns, no entity serialization or generated schema |
| user/dto/UserResponse.java | Record with the nine whitelisted legacy response fields |
| user/dto/UserUpdateRequest.java | Record with nullable fullName/avatarUrl/password; no speculative validation constraints |
| user/repository/UserRepository.java | UUID repository with explicit non-deleted lookup/page/count operations; no hard-delete use |
| user/service/UserService.java | List/detail/partial PUT/soft delete; explicit mutation timestamps and response results |
| user/controller/UsersController.java | Existing routes, endpoint-specific status codes and role/owner checks |

Start with a private mapping method in UserService; extract user/mapper/UserMapper.java only
if repetition warrants it. Reuse existing common.api records. Do not recreate the large C#
IUserService interface containing Authentication operations. Do not add registration DTOs,
new generic CRUD abstractions, role enums, relationships, optimistic locking or schema migrations.
An existing-token principal adapter and legacy password encoder are unresolved security
dependencies; their final files/scope require an explicit implementation decision before use.

## Authentication dependencies, not authorized implementation

- Password-changing PUT calls Microsoft.AspNet.Identity.PasswordHasher from
  Microsoft.AspNet.Identity.Core 2.2.4. Prove Java-produced hashes are accepted by the pinned
  C# verifier using synthetic passwords. A legacy verifier alone is insufficient. Exact hash
  format has not been independently verified; do not assume BCrypt or select a new algorithm.
- Existing JWT NameIdentifier is the UUID used for ownership; sub is email. Role authorization
  uses the URI role claim. Tokens also contain userId and plain role, but these are not an
  automatic substitute for the claims read by UsersController. Mock principals prove only
  authorization rules, not interoperability with real legacy bearer tokens.
- Signature/lifetime, zero skew, configured issuer/audience and nonblank/unrevoked JTI must
  be respected before exposing routes. Issuer/audience checks are conditional on configuration.
  The process-dev blacklist creates cross-process interoperability questions during coexistence.
- Registration creates the raw email/name, password hash, OTP/expiry, active=true,
  confirmed=false, role=user and creation audit fields. Email is sent before the insert.
  OTP verification clears OTP/expiry, confirms email and sets updated_at; retry count is unused.
- Login checks non-deleted, active and confirmed state. Token issuance/refresh rotates stored
  SHA256 refresh-token hashes and expiry and updates updated_at. Expired refresh clears refresh
  fields without explicitly updating updated_at. Logout clears refresh state, updates updated_at
  and blacklists JTI. Optional demo bootstrap also creates/updates User state.
- User PUT/delete must preserve all non-target security fields. Neither action revokes access
  tokens; token validation does not reload deleted/active/confirmed state. Changing this needs
  an explicit security/compatibility decision. Do not reproduce secret disclosure in error bodies.

Registration, login, OTP, token issuance, refresh, logout and bootstrap remain outside this slice.
Do not expose unprotected endpoints or silently reject password changes to avoid dependencies.

## Required tests before User can be COMPLETE

### Unit tests

- DTO mapping exposes exactly nine fields, retaining nulls, raw role/email and UUID; excludes
  hash, OTP, retry counter, refresh state and internal audit/delete fields.
- Pagination defaults and clamping, zero results, partial/final/beyond-last pages; flat wrapper
  metadata and messages. No search/filter/sort capability is invented.
- PUT omitted/null name/avatar retains values; empty/whitespace stores verbatim. Password
  null/empty/.NET whitespace retains hash; non-whitespace hashes the untrimmed input. Include
  Unicode whitespace such as NBSP to detect Java isBlank differences.
- Empty/no-op PUT still changes updated_at; other audit/security fields remain unchanged.
  Missing/deleted users produce the specified failures without writes.
- Soft delete changes only is_deleted/deleted_at; repeated delete fails as missing. Never hard-delete.
- Owner and admin matrix from MIG-004, including malformed/missing NameIdentifier, email sub,
  admin updating another account, admin self-delete and non-admin own-detail rejection.
- Hash interoperability vectors must test the writer against the legacy verifier before enabling
  password-changing PUT; keep test passwords synthetic and production credentials out of fixtures.

### PostgreSQL integration tests

Use the existing isolated PostgreSQL 16 Testcontainers infrastructure with no external fallback.
Add a users fixture only after provenance and actual schema differences are resolved. Keep
Hibernate validate; fixture DDL is container-only, never a production Flyway migration.

- Validate all 20 columns, lengths, nullability, defaults, UUID identity and timestamp round-trips;
  preserve existing IDs/audit values on loads/updates. Confirm schema and database collation.
- Verify unconditional email uniqueness including deleted rows, case/whitespace behavior and
  DB length failures; do not infer case-insensitive matching from Java code.
- Check non-deleted lookup/page/count, inclusion of inactive/unconfirmed rows, email ordering,
  boundary pages and total counts using deterministic isolated data.
- Reload after PUT/no-op/delete to verify exact changed and unchanged columns, nullable fields,
  and preservation of auth state. Verify no physical delete or related-table cascade.
- Document EF full-row update/concurrent-write behavior before choosing broader isolation or
  locking guarantees. Capture extreme Int32 pagination overflow rather than invent a cap.
- Confirm container datasource identity, safe configuration and absence of legacy/Flyway writes.

### API parity tests

Future files: src/test/java/com/zpantry/user/{service,controller,repository}/ and
src/test/resources/contracts/users/. Use separate isolated databases for legacy and Java runs.
Record commit, runtime/configuration, request, expected status/headers/body and database effects
for every capture; sanitize personal/security data without changing contract shape.

Cover all MIG-004 success/failure/auth matrix rows, missing/deleted users, pagination boundaries,
empty/null/partial PUTs, password updates, unsupported property writes and secret-field absence.
Also capture invalid GUID forms, Guid.Empty, malformed JSON, missing/null body, wrong field
types, unknown fields, invalid/overflowing integer queries, wrong content type, bearer challenge,
forbidden responses and DB exceptions. Assert exact message language, flat envelopes, nulls,
status and traceId differences. Compare dynamic timestamps/trace identifiers only with explicit
documented normalization; do not normalize away empty traceId or timestamp/nullability behavior.

Framework failure payloads and serialization precision remain unknown until captured. Do not
add fabricated examples. An isolated legacy host may run startup database creation/migrations:
review and isolate that host before any future capture, never point it at a developer/legacy DB.

## Decisions and completion gates

1. Obtain read-only actual schema evidence: qualified table/search_path, columns, defaults,
   constraints/indexes, collation, triggers/RLS and EF-history state. Resolve the PascalCase
   Db.sql versus snake_case EF/skeleton conflict without schema redesign.
2. Capture runtime contract/error evidence. Source controls the proposed happy-path behavior;
   generic convention examples (/api/v1, numeric IDs, PATCH, 404 missing) do not override it.
3. Approve a bounded existing-token validation/claim/revocation dependency and password encoder
   interoperability plan. This does not authorize migration of Authentication flows.
4. After implementation approval, create only the listed User files/tests and necessary approved
   adapters; update this analysis, API_MIGRATION.md, schema/status/risks/changelog and architecture
   or ADRs if an actual decision changes boundaries/compatibility.
5. Run Java 21 build/unit/PostgreSQL/API parity tests. Foundation's prior passing tests do not
   establish User parity. This documentation-only analysis did not rerun Maven or alter Foundation.

Stop here. No User implementation is authorized by this document.

## Evidence closure — 2026-09-18

This phase performed read-only/runtime probes and added evidence only. No User or Authentication
Java code was implemented and Foundation was not changed.

| Evidence gate | Classification | Result |
|---|---|---|
| Actual PostgreSQL users table | BLOCKED | dev PostgreSQL 17 is running, but it requires credentials. No connection environment variables or pgpass file were available; `psql -w` failed before a session. No SQL ran. |
| Legacy User HTTP captures | BLOCKED | The repository has no dev legacy source/runtime configuration. This host has .NET SDK 9 while the pinned source targets .NET 10; Docker was unavailable and no isolated legacy database exists. No backend was started and no JSON was fabricated. |
| Password hash generation | RESOLVED | Exact package 2.2.4 was executed with a synthetic password and independently verified on JDK 21. Format and strategy are established below. |
| User authorization claims | PARTIALLY_RESOLVED | Legacy source fully establishes required claims and controller checks. Java has Spring Security starter only: no SecurityFilterChain, JwtDecoder/authentication converter or blacklist adapter exists, so it cannot yet consume the identity. |

### Actual database probe

The only database found devly was a running `postgresql-x64-17` Windows service on the
default endpoint. No `ConnectionStrings*`, `DATABASE*`, `DB_*`, `PG*` or `POSTGRES*` environment
variable and no `%APPDATA%/postgresql/pgpass.conf` or `%USERPROFILE%/.pgpass` file was present.
An explicit no-prompt connection attempt as `postgres` returned `no password supplied`.
Because no authenticated session existed, transaction read-only mode and database identity
could not be established. Trying guessed credentials or reading server data files would not be
safe evidence. The 20 expected columns, defaults, constraints and indexes therefore remain
provisional exactly as documented in DATABASE_SCHEMA.md. No database was modified.

### HTTP capture probe

No dev `.sln` or legacy backend checkout was found under the ZPantry workspace. The pinned
host requires `authenticationconfig.json`, contains startup schema migration/create modes and
targets net10.0. This machine exposes only .NET SDK 9.0.305; the Docker Linux engine was not
available. Starting that host without an isolated database and secret-free configuration could
mutate a target database, so it was not attempted. The evidence directory records this blocker;
it contains no response JSON. Source-derived MIG-004 remains the contract proposal, not a runtime capture.

### Exact password format and compatibility vector

Both `AddUser` and `UpdateUserAsync` call a newly constructed
`Microsoft.AspNet.Identity.PasswordHasher` from NuGet `Microsoft.AspNet.Identity.Core` 2.2.4;
login calls the same class to verify. `PasswordHasher.HashPassword` delegates to the package's
`Crypto.HashPassword`. Package/runtime evidence establishes:

- PBKDF2 with HMAC-SHA1 via `Rfc2898DeriveBytes`;
- 1,000 iterations;
- cryptographically random 16-byte salt per hash;
- 32-byte derived subkey;
- binary payload `{ 0x00, salt[16], subkey[32] }`, 49 bytes total;
- standard Base64 encoding, normally 68 characters; byte 0 is the version/format marker;
- verification rejects a wrong payload length or marker before constant-work byte comparison.

The downloaded 2.2.4 DLL SHA-256 was
`99B0D1C98C1647EDB3FAEB2FC8217801DFE60FB4D47AD0FEB9B1073484D5E13C`.
The synthetic vector in `src/test/resources/contracts/auth/password-hash-v0-vector.properties`
was generated by that actual package. Package verification returned Success for the recorded
password and Failed for a changed password. JDK 21 decoded the 49-byte payload, derived
`PBKDF2WithHmacSHA1` with its embedded salt/1,000 iterations/256 bits, and matched the stored
subkey using a constant-time comparison (`java21Verification=true`). No real password/hash was used.

Recommended strategy: **A — temporarily generate and verify the exact legacy format in Java**
for shared-table compatibility. Keep this adapter narrowly scoped and mark it transitional.
Writing a new format during coexistence would make password-changing PUT incompatible with the
C# login path unless a separately approved dual-format/rehash rollout exists. Implementation
still requires Java tests that generate fresh random salts and C# cross-verification; the fixed
vector is a regression fixture, not a production salt.

### Exact claims consumed by UsersController

- User ID claim type is `ClaimTypes.NameIdentifier`, URI
  `http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier`; value is
  `user.Id.ToString()`, a canonical GUID string emitted by .NET. PUT calls `Guid.TryParse`; a
  missing, blank, malformed or unequal value returns the explicit 403. It does not fall back to
  `sub` or `userId`. `sub` is email; `userId` duplicates the UUID but is not consumed here.
- Admin role uses `ClaimTypes.Role`, URI
  `http://schemas.microsoft.com/ws/2008/06/identity/claims/role`. Token issuance trims and
  lowercases the stored role, defaulting blank to `user`, and also emits plain `role`.
  `[Authorize(Roles = "admin")]` protects list/detail/delete. PUT has only `[Authorize]`, then
  exact UUID ownership; administrators receive no bypass for another user's ID.
- The host clears JWT inbound/outbound claim maps and configures the URI role/name claim types,
  so Java must map the literal URI claims deliberately. It must not treat `sub` as the UUID or
  depend on Spring's default `SCOPE_` authorities. Existing validation also requires HS256
  signature, lifetime with zero clock skew, issuer/audience only when configured, and a nonblank,
  non-revoked `jti` checked against the process-dev blacklist.

Current Java cannot consume these claims: the dependency exists, but no JWT decoder, security
filter chain, claim-to-authority converter, owner-principal adapter or shared revocation provider
exists. A bounded security adapter is required by the User slice, while token issuance,
registration, refresh and logout remain out of scope. Cross-process blacklist parity is still
unresolved and cannot be simulated by a mock principal.

### Readiness decision

**User implementation is BLOCKED.** Password format is resolved and source-level endpoint/claim
behavior is strong, but implementing persistence against an unverified physical table or claiming
HTTP parity without executable captures would require guessing. These are required, not optional,
gaps. Once catalog evidence and an isolated legacy runtime are supplied, the next bounded scope is:

1. capture the read-only users catalog and build a matching container-only fixture;
2. capture the required User HTTP/auth/mutation matrix without normalizing payloads;
3. approve and implement only the minimal legacy JWT consumer/claim mapping/revocation boundary;
4. implement BaseEntity, User, DTOs, repository, service and controller plus the planned tests;
5. implement the narrow version-0 password adapter used only by User PUT/shared legacy hashes;
6. run JDK 21 unit, PostgreSQL and API parity verification before changing User status.

The expected Java file list remains the one above, plus a narrowly named legacy password adapter
and the minimum security identity adapter/configuration proven by the captured token behavior.

Verification after these evidence/documentation changes used Microsoft JDK 21.0.12. Compilation
and all four unit tests passed. The three existing Foundation integration tests failed before
database startup because Testcontainers could not detect the currently unavailable Docker engine;
none was skipped or altered. This does not replace the earlier successful Foundation run or
change Foundation status, but Docker must be available for the next container-backed verification.

## Disposable runtime closure — 2026-09-18

Docker recovered and the full Java `mvnw verify` passed on JDK 21.0.12: four unit and three
PostgreSQL/pgvector integration tests, no failures/errors/skips. The pinned legacy commit was then
built with official .NET 10 images and run on a private Docker network against a disposable
PostgreSQL 16.14/pgvector 0.8.2 database. Its official migration and three-account bootstrap
completed; no real database or external service was referenced.

Synthetic accounts were seeded by `EnsureDemoAccountsAsync`, which generated application UUIDs,
UTC audit values and hashes using the actual legacy PasswordHasher:

| Identity | Role | Active/confirmed | Purpose |
|---|---|---|---|
| admin@test.dev | admin | true/true | list/detail/delete |
| owner@test.dev | user | true/true | owner/PUT probes |
| other-user@test.dev | user | true/true | cross-owner/role rejection |

All three logged in successfully through the real login endpoint. Tokens and refresh tokens were
used transiently and are absent from fixtures. The capture contains 23 HTTP cases, raw bodies and
headers, a provenance manifest, disposable catalog and mutation snapshots under
`src/test/resources/contracts/users/evidence/captured-2026-09-18/`.

### Confirmed runtime behavior

- Admin list returns 200 and flat PagedResponse fields before ApiResponse fields. Defaults are
  page 1/size 10; zero values normalize to 1/1; size 101 clamps to 100. Rows order by email.
- Unauthenticated protected requests return 401 with `WWW-Authenticate: Bearer` and an empty
  body. Authenticated non-admin list/detail/delete returns 403 with an empty body.
- Admin existing detail returns 200. Missing and soft-deleted detail return 200 with
  `success=false`, `User not found.`, null data/errors and empty traceId.
- Admin DELETE returns 200 success. It changes only is_deleted/deleted_at. Repeated delete returns
  a 200 failure wrapper. Subsequent detail excludes the deleted row.
- Response User data contains exactly the documented nine fields; no password, OTP, refresh or
  internal audit/delete fields appeared.

### Blocking PUT source/runtime conflict

Every PUT made with the real login-issued owner token returned the controller's explicit 403,
including a route UUID equal to the token's emitted NameIdentifier. Partial, empty-object,
empty-string and password requests all left the database row byte-for-field unchanged. Admin PUT
to another user also returned that wrapper; anonymous PUT returned the empty middleware 401.
A separately signed test token carrying the expected owner claim also returned 403.

Therefore the executable pinned runtime does not expose the source-intended successful owner PUT.
The source still clearly defines the mutation logic, but runtime fixtures cannot confirm it and
the password PUT cannot be followed by login with an updated password. The original password
continued to log in and the hash remained a valid 49-byte marker-0 payload.

This contradiction must be resolved explicitly before production User implementation: either
preserve the observed always-403 PUT behavior for strict runtime compatibility, or approve fixing
legacy owner-claim consumption and implement the source-intended partial update contract. The
latter is a deliberate compatibility/security correction, not an inference.

### Updated readiness

| Gate | Classification |
|---|---|
| Docker/Testcontainers | VERIFIED |
| Disposable legacy runtime | VERIFIED |
| Actual shared users table | BLOCKED (disposable migration schema verified; real catalog unavailable) |
| Legacy HTTP contracts | PARTIALLY_RESOLVED (read/delete/auth captured; successful PUT unreachable) |
| Password compatibility | RESOLVED |
| JWT identity requirements | PARTIALLY_RESOLVED (claims known; owner consumption fails at runtime) |

This was the 2026-09-18 readiness conclusion and is superseded by the claims diagnosis below.
Production cutover remains blocked on the real users catalog.

## ClaimsPrincipal evidence closure — 2026-09-19

The owner rejection is now explained and reproducible with the pinned legacy runtime and its
resolved identity packages (`Microsoft.IdentityModel.JsonWebTokens` and
`System.IdentityModel.Tokens.Jwt` 8.0.1). Temporary instrumentation existed only in the
disposable legacy copy. Redacted evidence is under
`src/test/resources/contracts/users/evidence/claims-diagnosis-2026-09-19/`.

The login JWT contains `sub=owner@test.dev`, `userId=<owner UUID>`, the literal
`ClaimTypes.NameIdentifier` URI with the owner UUID, `email=owner@test.dev`, `jti`, the literal
role URI with `user`, and plain `role=user`. `JwtBearerOptions.MapInboundClaims` is `true`; the
active handler is `Microsoft.IdentityModel.JsonWebTokens.JsonWebTokenHandler`, whose mapping is
also `true` and whose default inbound map has 73 entries. Clearing the static inbound/outbound
maps on `JwtSecurityTokenHandler` therefore does not disable the active handler's mapping.

`JsonWebTokenHandler` maps raw `sub` to the NameIdentifier URI and raw `email`/`role` to their
Microsoft claim URIs. It preserves the already URI-named NameIdentifier and role claims. The
resulting principal contains these two NameIdentifier claims in order:

1. `http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier = owner@test.dev`
2. `http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier = <owner UUID>`

The controller code is exactly:

```csharp
var currentUserIdClaim = User.FindFirstValue(ClaimTypes.NameIdentifier);
if (!Guid.TryParse(currentUserIdClaim, out var currentUserId) || currentUserId != id)
{
    return StatusCode(403, ApiResponse<UserDto>.Fail("Bạn chỉ được phép cập nhật thông tin của chính tài khoản mình.", traceId: HttpContext.TraceIdentifier));
}
```

There is no fallback. `FindFirstValue` resolves the first duplicate claim, the email.
`Guid.TryParse("owner@test.dev", out ...)` returns false, so the OR short-circuits and the
controller returns its explicit 403. The captured route UUID was
`9a833dce-849f-43ee-aa6e-6760ffcf3e54`; the later UUID claim matched it but was never selected.

An independently HS256-signed token using the same disposable issuer, audience, key and
evidence-backed identity values, with `sub=owner@test.dev` and `nameid=<owner UUID>`, produced
the same ordered duplicate claims and 403. This proves the separately signed case fails by the
same mechanism for that token shape.

### Classification and alternatives

This is a claim-mapping/configuration bug, not intentional authorization behavior and not a
database or disposable-environment artifact. The issuer deliberately emits an owner UUID, the
controller deliberately compares it with the route, and other controllers already use `userId`,
then NameIdentifier, then `nameid` fallbacks.

**Option A — strict observed compatibility:** Java returns the explicit owner 403 for every
tested login-token PUT. This exactly matches captured behavior, but leaves profile/password
update unusable, requires parity tests to assert the defect, and gives the frontend no working
self-update path.

**Option B — intentional legacy bug fix (recommended, approval pending):** Java extracts the
owner UUID deterministically from validated `userId`, with a UUID-valued NameIdentifier fallback,
and permits PUT only when it equals the route UUID. Wrong-owner and admin-on-another-user PUT stay
403; there is no admin bypass. This differs from the captured defective 403 and must be recorded
in MIG-004 and accepted through proposed ADR-012. Tests must cover duplicate NameIdentifier
claims, email `sub`, malformed/missing UUID identity, owner success, wrong owner and admin-other.

### Readiness and minimum identity boundary

User implementation is **READY WITH DOCUMENTED LIMITATIONS**. Pinned entities, EF mappings,
migrations, the generated disposable catalog, runtime operations, HTTP fixtures and the resolved
password format are sufficient for dev implementation. Starting implementation still requires
approval of the slice and the Option A/Option B choice.

Production cutover remains **BLOCKED** until the actual shared users-table catalog is compared.
Unverified production assumptions include physical types/nullability/defaults, indexes and unique
constraints, collation/case behavior, triggers/RLS/grants, migration history, data quality, role
casing, soft-delete population, and password/refresh-token value variants.

The minimum Java dependency is an immutable `AuthenticatedUser` (or `LegacyJwtIdentity`) carrying
UUID, roles and JTI; a `LegacyClaimConverter` that extracts UUID deterministically only after full
bearer validation; and a narrow `TokenRevocationChecker`. The User slice must not issue tokens or
implement login, registration, refresh rotation, logout storage or OTP. A no-op revocation
implementation is not production-safe: the adapter must consult the selected shared revocation
mechanism or protected Java routes must remain disabled during mixed-runtime cutover.

## Implementation completion — 2026-09-19

Option B was explicitly approved and ADR-012 is accepted. The proposed persistence, repository,
service, DTO, controller, password and identity components are implemented. The owner-claim
converter prefers validated `userId`, accepts only an unambiguous UUID NameIdentifier fallback,
and never treats email `sub` as UUID. The complete Java 21 test lifecycle verifies the slice in
isolated PostgreSQL 16.

User status is **COMPLETE FOR dev MIGRATION SCOPE**. The two documented production gates remain:
actual shared-catalog comparison and interoperable cross-runtime revocation.
