# Authentication Migration Analysis

Date: 2026-09-19  
Legacy source: `a010fdc5894176596bb195e4fef66db2c09496f1`  
Phase: evidence closure and compatibility specification; no Java Authentication implementation

## Evidence and authority

The complete legacy controller, service, repositories, DTOs, JWT setup, blacklist, user mapping,
email adapter and environment configuration were traced. Runtime observations come from the
isolated capture described in `src/test/resources/contracts/auth/evidence/2026-09-19/README.md`.
The generated disposable catalog is evidence of EF behavior, not proof of the shared production
catalog. ADR-012 remains authoritative for deterministic Java identity extraction.

## Exact public contract

All routes use `application/json` and the existing `/api/Auth` casing. DTO properties are serialized
as camel case. The request classes initialize missing strings to `""` and contain no validation
attributes.

| Endpoint | Auth | Request | Success | Verified failures | Effects |
|---|---|---|---|---|---|
| `POST /api/Auth/register` | anonymous | `fullName`, `email`, `password`: string | 200 `ApiResponse<object>`, null data | duplicate/email failure 500 wrapped; malformed JSON 400 framework problem details | Sends verification email, then inserts user |
| `POST /api/Auth/verify-otp` | anonymous | `otpCode`, `email`: string | 200 null-data envelope | wrong, expired, absent, already confirmed: 400 same envelope/message | Confirms account and clears OTP on success |
| `POST /api/Auth/login` | anonymous | `email`, `password`: string | 200 `ApiResponse<AuthResponse>` | credential/absent/deleted 401 same message; inactive 401 distinct; unconfirmed 401 distinct; malformed 400 problem details | Rotates refresh token and updates user |
| `POST /api/Auth/refresh-token` | anonymous | `refreshToken`: string | 200 `ApiResponse<AuthResponse>` | missing/blank, invalid/deleted/old token, expired, inactive/unconfirmed: 401 with source-specific messages | Valid use rotates both tokens; expired use clears stored refresh state |
| `POST /api/Auth/logout` | bearer | empty body accepted | 200 null-data envelope, `Logout successful.` | revoked/repeated token rejected by middleware as bare 401; service maps invalid token to wrapped 401 | Clears stored refresh state and blacklists access JTI |

Runtime proves `{}` registration succeeds and creates a user with blank name/email and a hash of
the blank password. This unsafe behavior is part of the observed compatibility baseline and needs
an explicit compatibility/security decision before implementation. `{}` login returns the generic
invalid-credential 401. Error envelopes include `success`, `message`, `data`, `errors`, `traceId`
and `timestamp`; malformed JSON uses ASP.NET problem details instead.

`AuthResponse` is exactly `accessToken`, `expiresAt`, `fullName`, `email`, `refreshToken`, `role`.
No entity or stored hash is returned.

## Registration and OTP

Email lookup is exact and excludes soft-deleted rows; there is no trimming, case folding or
normalization. A duplicate throws `Email already exists.` and becomes HTTP 500. Registration uses
the existing ASP.NET Identity v2 hasher, assigns role `user`, active `true`, confirmed `false`,
`otpRetryCount=0`, `createdAt=UtcNow`, `updatedAt=null`, and leaves refresh/deletion fields null or
false. OTP is `Random.Shared.Next(100000, 999999).ToString()`: six decimal digits, range
100000–999998, non-cryptographic, expiring after five minutes.

The actual provider is Resend through the Resend SDK. The legacy `Gmail` configuration name and
success copy are misleading; `Gmail.Password` holds the Resend API key, while Host/Port/SSL are
unused. Delivery occurs before `AddUser`. A delivery exception produces a wrapped 500 and no row;
successful external delivery followed by insert failure can therefore send an unusable OTP.

Verification compares exact email and OTP strings. Expiration is rejected only when
`otpExpiredAt < UtcNow`; equality is not explicitly expired. All failures return the same 400 and
make no mutation. `otpRetryCount` is never incremented. Success sets confirmed true, clears code
and expiry, and sets `updatedAt=UtcNow`. There is no resend route; re-registering an unconfirmed
address is treated as a duplicate.

## Password interoperability

Register and User PUT generate ASP.NET Identity v2 hashes. Login verifies them. The implemented
Java `LegacyPasswordHasher` already provides the compatible v0 PBKDF2-HMAC-SHA1 behavior and can
be reused directly; Authentication must not add a second encoder model. Existing synthetic vector
tests remain the compatibility proof.

## JWT issuance and validation

Access tokens use HS256 and require a UTF-8 secret of at least 32 bytes. Configured issuer and
audience are emitted; validation requires either only when its configured value is nonblank.
Lifetime is configured in minutes (legacy default 60), with `nbf=UtcNow`, `exp=UtcNow+lifetime`,
zero validation clock skew and no explicit `iat`. JTI is `Guid.NewGuid().ToString("N")`.

Claims in emission order are: `sub` email, `email`, `jti`, `userId` UUID, literal NameIdentifier
URI UUID, literal Name URI full name, `fullName`, lower-case `isEmailConfirmed`, literal Role URI,
and short `role`. Role is trimmed/lowercased and defaults to `user`; full name defaults to empty.
Runtime confirms a 3,600-second `nbf`/`exp` interval. `AuthResponse.expiresAt` is computed with a
separate clock read and may differ slightly from JWT `exp`. Java must preserve the wire claims but
must retain ADR-012 extraction and must not reproduce inbound claim shadowing.

## Refresh tokens

Generation uses `RandomNumberGenerator.GetBytes(64)`, Base64, URL-safe substitutions, and removal
of `=` padding. Runtime confirms 86 characters and a decoded 64-byte value. Only uppercase
SHA-256 hex (64 characters) is stored. Raw tokens appear only in responses. Lifetime is configured
in days (legacy default 7).

Login and successful refresh always issue both a new access token and a new refresh token, replace
the previous hash/expiry, set `updatedAt=UtcNow`, and leave `updatedBy` unchanged. Reuse of the old
token then returns generic invalid-token 401. Missing/whitespace returns `Refresh token is required.`
Deleted users look invalid because repository lookup excludes them. Inactive or unconfirmed users
return `Account is not allowed to refresh token.` without clearing the token. Expired tokens clear
hash and expiry and persist, but do not update `updatedAt`.

## Logout and revocation

Bearer authentication runs before the controller. The service reads raw `jti` and email from the
already validated JWT. Missing identity yields wrapped 401. When a non-deleted user is found,
logout clears refresh hash/expiry and sets `updatedAt`; a missing/deleted user does not prevent JTI
revocation. The blacklist is a static process-dev `ConcurrentDictionary<string, DateTimeOffset>`
mapping JTI to token expiry. Lookup lazily removes expired entries; there is no periodic cleanup,
persistence or cross-instance sharing.

Runtime proves the same token is bare-401 after logout and on repeated logout, then is accepted
again after the C# container restarts. Therefore the current Java `TokenRevocationChecker` is only
a required boundary; it does not solve interoperability.

## Database mutation matrix

`—` means unchanged. Values describe successful paths unless a failure is named.

| Column | Register | Verify OTP | Login | Refresh | Logout |
|---|---|---|---|---|---|
| `id` | new UUID | — | — | — | — |
| `full_name`, `email` | request values | — | — | — | — |
| `password_hashed` | new Identity-v2 hash | — | — | — | — |
| `created_at` | UTC now | — | — | — | — |
| `updated_at` | null | UTC now on success | UTC now | UTC now on success; unchanged when expired | UTC now if user found |
| `updated_by` | null | — | — | — | — |
| `deleted_at`, `deleted_by`, `is_deleted` | null/null/false | — | — | — | — |
| `otp_code` | new six-digit value | null on success | — | — | — |
| `otp_expired_at` | UTC now + 5m | null on success | — | — | — |
| `otp_retry_count` | 0 | unchanged on every outcome | — | — | — |
| `is_email_confirmed` | false | true on success | — | — | — |
| `is_active` | true | — | — | — | — |
| `role` | `user` | — | — | — | — |
| `refresh_token_hash` | null | — | replace | replace; clear when expired | clear if user found |
| `refresh_token_expires_at` | null | — | now + configured days | replace; clear when expired | clear if user found |

Registration delivery failure inserts nothing. OTP failures, login failures, invalid/old refresh,
state-rejected refresh and middleware-rejected logout make no database mutation.

## Cross-runtime revocation

Scenario A, C# issue → C# logout → Java use: Java accepts the token unless its checker reads a
shared revocation record written by C#. Scenario B, future Java issue → Java logout → C# use: C#
accepts the token because its static dictionary cannot see Java state. Scenario C, concurrent use:
each runtime can make a different revocation decision, and multiple C# instances also disagree.

| Strategy | C# / Java impact | Infrastructure | Compatibility and security | Cutover impact |
|---|---|---|---|---|
| A. Preserve dev memory | Keep C# dictionary; Java uses dev checker | none | Matches single-process legacy behavior but logout is not globally effective and restart revives tokens | Safe only with strict token-affinity/single issuer-consumer routing and a bounded token drain |
| B. Shared revocation store | Adapt both runtimes to write/read JTI with expiry | PostgreSQL table or another already-operated shared store | Makes logout consistent across processes; adds availability/cleanup concerns and changes C# internals without changing API | Supports gradual bidirectional coexistence |
| C. Authentication-owner routing and drain | One runtime owns issuance/logout and all bearer-protected traffic for old tokens; cut over after maximum access lifetime | gateway/routing rules; no new store | Preserves current mechanism within one process if routing affinity is enforceable; concurrency across runtimes is intentionally prevented | Lowest schema impact, but constrains rollout and needs a maintenance/drain window |

Recommendation: use Option C for a short controlled cutover if infrastructure can guarantee that
every access token is validated and logged out by the same single auth-owning runtime, then switch
all traffic atomically and wait at least the maximum access-token lifetime before retiring it.
Choose Option B if APIs must accept the same tokens concurrently across C# and Java. PostgreSQL is
the smallest already-required shared dependency, but its table/lifecycle design needs explicit
approval and real-schema review. Option A is not adequate for mixed-runtime operation.

## Proposed Java ownership and types

`authentication` owns `AuthenticationController`, orchestration service, request/response records,
JWT issuance, refresh-token generation/hash handling, OTP policy, the email verification port and
revocation write service. `user` continues to own `UserEntity`, `UserRepository`,
`LegacyPasswordHasher`, `AuthenticatedUser` and its established identity conversion. A neutral
security package may own the shared revocation interface only if both features require it. The
dependency direction is authentication → user/common; user must not depend on authentication.

Planned DTO records preserve names exactly: `RegisterRequest(fullName,email,password)`,
`VerifyOtpRequest(otpCode,email)`, `LoginRequest(email,password)`,
`RefreshTokenRequest(refreshToken)`, and
`AuthResponse(accessToken,expiresAt,fullName,email,refreshToken,role)`. Entity types never cross the
controller boundary. Validation must first resolve the observed empty-input compatibility/security
decision; adding conventional constraints silently would change the contract.

Implementation security requirements are: secure OTP and refresh randomness (an intentional OTP
change requires approval), no plaintext password/refresh persistence or logging, full HS256 and
lifetime validation, configured issuer/audience behavior, deterministic UUID identity extraction,
normalized role emission, soft-delete/state checks matching the matrix, and exact SHA-256 encoding.

## Readiness

| Capability | Readiness | Qualification |
|---|---|---|
| Register | PARTIALLY_RESOLVED | Exact behavior captured; empty-input compatibility and weak OTP require an explicit decision |
| OTP verification | PARTIALLY_RESOLVED | Exact behavior captured; cryptographic RNG requirement conflicts with legacy generator |
| Login | READY | Source/runtime contract and password interoperability resolved |
| Refresh token | READY | Generation, storage, rotation, errors and mutations resolved |
| Logout | PARTIALLY_RESOLVED | Single-process behavior resolved; shared revocation strategy undecided |
| JWT issuance | READY | Claims, algorithm, validation and lifetimes resolved |
| Cross-runtime revocation | BLOCKED | Requires approved Option B or enforceable Option C rollout |

Authentication implementation readiness is **READY WITH DOCUMENTED LIMITATIONS**: login, token
issuance and refresh are specified, while register/OTP security deviations and revocation topology
must be decided during implementation planning. Production cutover readiness is **BLOCKED** by
cross-runtime revocation, actual shared users-catalog verification, and real deployment
issuer/audience/key configuration validation.

The recommended implementation scope is one Authentication vertical slice integrating the existing
User repository, password hasher and claim model: DTOs and controller, register/OTP orchestration
behind an email port, JWT/refresh services, and the approved revocation strategy, followed by
PostgreSQL integration and fixture parity tests. It must exclude User duplication and all other
feature modules.
