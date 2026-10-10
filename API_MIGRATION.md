# API Migration Guide

> Evidence clarification (2026-09-16): MIG-001 through MIG-003 below are illustrative
> template examples, not delivered ZPantry migrations or approved contract changes.
> Their numeric IDs, /api/v1 paths, sample fields and removal dates must not drive this
> compatibility migration. The first source-verified User analysis is MIG-004 (PENDING).

## Purpose

File này là nguồn theo dõi chính thức cho mọi thay đổi API trong giai đoạn migration của dự án.

Mục tiêu là giúp Frontend xác định nhanh:

- endpoint nào đã thay đổi;
- endpoint cũ là gì;
- endpoint mới là gì;
- HTTP method có thay đổi hay không;
- request parameters/body có thay đổi hay không;
- response structure có thay đổi hay không;
- Frontend cần sửa gì;
- endpoint cũ còn hoạt động hay đã bị loại bỏ.

---

## Mandatory Rule

Trong giai đoạn migration:

> Bất kỳ thay đổi nào ảnh hưởng đến API contract đều PHẢI được ghi vào file này.

Bao gồm nhưng không giới hạn:

- đổi endpoint path;
- đổi HTTP method;
- đổi path variable;
- đổi query parameter;
- đổi request body;
- đổi request DTO;
- đổi response DTO;
- đổi tên field;
- thêm/xóa field;
- đổi kiểu dữ liệu;
- đổi status code;
- đổi authentication/authorization requirement;
- đổi pagination;
- đổi sorting/filtering;
- đổi error response;
- endpoint bị deprecated;
- endpoint bị xóa;
- endpoint được thay thế bằng endpoint khác.

Không được merge thay đổi API nếu file này chưa được cập nhật.

---

# Migration Entries

Mỗi thay đổi API phải dùng format sau.

## [MIG-XXX] Short description

**Status:** `PENDING | MIGRATED | DEPRECATED | REMOVED`

**Breaking Change:** `YES | NO`

**Date:** `YYYY-MM-DD`

### Old API

```http
METHOD /old/path
```

### New API

```http
METHOD /new/path
```

### Changes

- Change 1
- Change 2
- Change 3

### Old Request

```json
{}
```

### New Request

```json
{}
```

### Old Response

```json
{}
```

### New Response

```json
{}
```

### Frontend Action Required

Frontend cần:

1. Replace old endpoint with new endpoint.
2. Update request mapping if necessary.
3. Update response mapping if necessary.
4. Update error/status-code handling if necessary.

### Compatibility

Old endpoint:

```text
STILL AVAILABLE | DEPRECATED | REMOVED
```

Removal target:

```text
YYYY-MM-DD | N/A
```

### Notes

Additional migration information if needed.

---

# Example

## [MIG-001] Replace legacy user detail endpoint

**Status:** `MIGRATED`

**Breaking Change:** `YES`

**Date:** `2026-09-16`

### Old API

```http
GET /api/getUserById?id={id}
```

### New API

```http
GET /api/v1/users/{id}
```

### Changes

- Endpoint changed from action-based naming to REST resource naming.
- `id` changed from query parameter to path variable.
- API version `/v1` was added.
- Response now uses `UserResponse` instead of returning the persistence entity directly.

### Old Request

```http
GET /api/getUserById?id=10
```

### New Request

```http
GET /api/v1/users/10
```

### Old Response

```json
{
  "id": 10,
  "username": "khang",
  "email": "example@email.com",
  "passwordHash": "..."
}
```

### New Response

```json
{
  "id": 10,
  "username": "khang",
  "email": "example@email.com"
}
```

### Frontend Action Required

Frontend cần:

1. Replace:

```text
/api/getUserById?id={id}
```

with:

```text
/api/v1/users/{id}
```

2. Pass `id` as part of the URL path instead of query parameters.

3. Remove any dependency on `passwordHash`.

### Compatibility

Old endpoint:

```text
DEPRECATED
```

Removal target:

```text
2026-10-01
```

---

# Example — Request DTO Change

## [MIG-002] Rename registration request fields

**Status:** `PENDING`

**Breaking Change:** `YES`

**Date:** `2026-09-16`

### API

```http
POST /api/v1/users
```

### Old Request

```json
{
  "userName": "khang",
  "mail": "example@email.com",
  "pwd": "password"
}
```

### New Request

```json
{
  "username": "khang",
  "email": "example@email.com",
  "password": "password"
}
```

### Changes

```text
userName -> username
mail     -> email
pwd      -> password
```

### Frontend Action Required

Replace request object fields:

```text
userName → username
mail     → email
pwd      → password
```

### Compatibility

Old request fields:

```text
REMOVED
```

---

# Example — Response Field Change

## [MIG-003] Rename product price response field

**Status:** `MIGRATED`

**Breaking Change:** `YES`

**Date:** `2026-09-16`

### API

```http
GET /api/v1/products/{id}
```

### Old Response

```json
{
  "id": 1,
  "name": "Keyboard",
  "priceValue": 500000
}
```

### New Response

```json
{
  "id": 1,
  "name": "Keyboard",
  "price": 500000
}
```

### Frontend Action Required

Replace:

```text
product.priceValue
```

with:

```text
product.price
```

---

# Example Status Index — not actual migration progress

| ID | API | Breaking | Status | Frontend Action |
|---|---|---:|---|---|
| MIG-001 | `GET /api/v1/users/{id}` | Yes | Migrated | Replace old user endpoint |
| MIG-002 | `POST /api/v1/users` | Yes | Pending | Rename request fields |
| MIG-003 | `GET /api/v1/products/{id}` | Yes | Migrated | Rename response field |

---

# Rules for Backend Developers and AI Agents

Before changing an API:

1. Inspect the existing API contract.
2. Determine whether the change affects Frontend.
3. If the contract changes, create or update a migration entry in this file.
4. Assign the next `MIG-XXX` identifier.
5. Document both the old and new contract.
6. Clearly describe the exact Frontend replacement required.
7. State whether the change is breaking.
8. State whether the old contract remains temporarily compatible.

After changing an API:

1. Update the migration entry with the final implementation.
2. Verify example requests and responses against the implementation.
3. Mark the correct migration status.
4. Never mark an API as `MIGRATED` if the documented contract does not match the implementation.

---

# Definition of an API Contract Change

Treat a modification as an API contract change whenever a consumer may need to change its code.

For example:

```text
Controller method renamed internally
→ NOT necessarily an API contract change

Service implementation changed
→ NOT an API contract change

/api/users/{id}
→ /api/v1/users/{id}
→ API contract change

email
→ emailAddress
→ API contract change

HTTP 200
→ HTTP 204
→ API contract change

response List<User>
→ Page<UserResponse>
→ API contract change
```

When uncertain, document the change.

---

## [MIG-004] User compatibility slice — source analysis only

**Status:** `MIGRATED` (dev migration scope verified; production cutover blocked)
**Breaking Change:** `YES` for the explicitly approved owner-PUT bug correction
**Date:** `2026-09-16`
**Legacy API:** present in pinned source; deployment/runtime not probed
**Java API:** implemented and verified against isolated PostgreSQL/API/security tests
**Frontend action:** retain existing routes and UUID identifiers; owner PUT now works as intended.

### Evidence and limits

Pinned repository: ZPantry/ZPantry-Backend, main rechecked at
`a010fdc5894176596bb195e4fef66db2c09496f1`.

- [UsersController](https://github.com/ZPantry/ZPantry-Backend/blob/a010fdc5894176596bb195e4fef66db2c09496f1/AuthenticationModule/Controllers/UsersController.cs)
- [UserService](https://github.com/ZPantry/ZPantry-Backend/blob/a010fdc5894176596bb195e4fef66db2c09496f1/AuthenticationModule/Services/Implementations/UserService.cs)
- [UserRepository](https://github.com/ZPantry/ZPantry-Backend/blob/a010fdc5894176596bb195e4fef66db2c09496f1/AuthenticationModule/Repositories/Implementations/UserRepository.cs)
- [User DTOs](https://github.com/ZPantry/ZPantry-Backend/blob/a010fdc5894176596bb195e4fef66db2c09496f1/AuthenticationModule/DTOs/UserDtos.cs)
- [Common response types](https://github.com/ZPantry/ZPantry-Backend/tree/a010fdc5894176596bb195e4fef66db2c09496f1/AuthenticationModule/Contracts/Common)
- [Host authentication/serialization setup](https://github.com/ZPantry/ZPantry-Backend/blob/a010fdc5894176596bb195e4fef66db2c09496f1/ZPantry_Backend/Program.cs)

The tables below are source-derived contracts, not captured response payloads. No live
OpenAPI export or legacy response fixtures are available in this Java project. Framework
binding, authentication challenges, exact timestamp formatting, casing/coercion and generic
exception bodies need runtime parity evidence. Persistence mapping is maintained in
[DATABASE_SCHEMA.md](docs/DATABASE_SCHEMA.md#user-slice-mapping-analysis--2026-09-16),
implementation/test planning in [USER_MIGRATION_ANALYSIS.md](docs/USER_MIGRATION_ANALYSIS.md).

### Exact route and operation matrix

Proposed Java routes/methods equal legacy routes/methods. Do not introduce /api/v1, PATCH,
POST /api/users, email/role management, search or sorting controls.

| Method / route | Request | Authorization | Source-defined result |
|---|---|---|---|
| GET /api/users | pageIndex:int=1, pageSize:int=10 | authenticated role admin | 200 PagedResponse<UserResponse>, including empty page |
| GET /api/users/{id:guid} | UUID path | authenticated role admin | 200 ApiResponse<UserResponse>; missing/deleted user also 200 with success=false |
| PUT /api/users/{id:guid} | UUID path, JSON UserUpdateRequest | authenticated AND NameIdentifier parses as UUID equal to path id | 200 success; 400 missing/deleted user; explicit 403 if identity missing/malformed/different |
| DELETE /api/users/{id:guid} | UUID path, no request DTO | authenticated role admin | 200 ApiResponse<object> with data=null; missing/deleted user also 200 with success=false |

`:guid` is ASP.NET's route constraint, not a literal segment in the URL. Accepted textual
GUID forms and invalid-path handling must be captured; a Spring UUID conversion error
must not silently replace legacy route non-match behavior. Guid.Empty is not explicitly
rejected by source. Valid-but-nonexistent UUIDs take the missing-user paths above.

### User response data contract

One response DTO is used for list items, detail and successful update. Proposed Java name:
`UserResponse` record. These are the JSON names implied by normal ASP.NET web serialization;
Program.cs registers AddControllers without custom JSON naming/null settings.

| JSON field | Legacy type / nullability | Proposed Java type | Meaning |
|---|---|---|---|
| id | Guid, non-null | UUID | existing identifier |
| fullName | string? | String nullable | preserve null/empty/whitespace |
| email | string, non-null | String | raw stored email; no normalization in mapper |
| avatarUrl | string? | String nullable | preserve null/empty/whitespace |
| isEmailConfirmed | bool | boolean | explicitly exposed by legacy DTO |
| isActive | bool | boolean | explicitly exposed by legacy DTO |
| role | string, non-null | String | raw stored role, not token-normalized role |
| createdAt | DateTime, non-null | Instant | UTC creation time |
| updatedAt | DateTime? | Instant nullable | latest explicit update time, including auth writes |

There is no username field and no numeric ID. The public role/active/confirmed fields are
intentional legacy exposure, not permission to expose other security state. Never serialize
User entities. Exclude passwordHashed, password, otpCode, otpExpiredAt, otpRetryCount,
refreshTokenHash, refreshTokenExpiresAt, createdBy, updatedBy, deletedAt, deletedBy and
isDeleted. The password request field must not be logged or echoed.

### User update request and partial semantics

`UserUpdateRequest` is a record with exactly three nullable String components:
`fullName`, `avatarUrl`, `password`. Legacy UpdateUserRequest has no validation attributes.

| Input | Source-defined effect |
|---|---|
| fullName omitted or null | retain current full_name |
| avatarUrl omitted or null | retain current avatar_url |
| fullName/avatarUrl empty or whitespace string | store exact supplied value; no trim |
| password omitted, null, empty or .NET whitespace-only | retain password_hashed |
| password non-whitespace | hash original untrimmed value with Microsoft.AspNet.Identity.PasswordHasher and replace password_hashed |
| empty JSON object or all no-op fields | still assign updated_at=UtcNow and save |

Null does not clear profile fields. Do not replace PUT with PATCH or turn it into full
replacement. No old-password confirmation, password length/strength check, email update,
role update, activation update, verification change, token reset or session revocation is
performed by this method. FullName/avatar DB length violations are not prevalidated by the
User DTO/service; adding @Size/@NotBlank/@URL or a password policy would change behavior.
The exact .NET IsNullOrWhiteSpace character set must be checked before using Java isBlank
as a substitute (including nonbreaking spaces). Unknown JSON properties have no mapped
setter in this request; capture framework ignore/reject/coercion behavior before certifying it.

Password-changing PUT is blocked for implementation until hash **generation** interoperability
with the still-running C# verifier is proven using safe synthetic vectors. A verifier alone
is insufficient; do not write BCrypt into a shared legacy users table.

### Pagination / selection

Service normalization: pageIndex=max(input,1); pageSize=clamp(input,1,100). Return normalized
values in metadata. Repository selects is_deleted=false, orders by Email ascending in the
database, then Skip((pageIndex-1)*pageSize), Take(pageSize). It separately counts all
non-deleted users. Inactive and unconfirmed rows are included. No search, email filter,
active/role filter, sort parameter or client-selected order is implemented.

Paged envelope fields are the six ApiResponse fields plus pageIndex, pageSize, totalItems,
totalPages, hasNextPage and hasPreviousPage at the top level. data is an array. totalPages
is ceil(totalItems/pageSize), hasNextPage is pageIndex<totalPages, hasPreviousPage is
pageIndex>1; an out-of-range page is empty but retains its requested normalized pageIndex.
An empty database has totalPages=0 and page 1 has both navigation flags false.

Do not expose Spring Page or change to zero-based page numbers. DB collation governs email
ordering; do not lowercase/trim emails or add a tie-breaker without evidence. Count and page
are separate queries, so concurrent writes can make them inconsistent. Int32 binding and
large-page offset overflow are uncharacterized edge cases; do not invent a different cap.

### Authorization matrix

| Caller | List | Detail (own/other) | PUT own | PUT other | DELETE own/other |
|---|---|---|---|---|---|
| No valid authenticated principal | bearer challenge | bearer challenge | bearer challenge | bearer challenge | bearer challenge |
| Authenticated non-admin with valid owner claim | forbidden | forbidden, even own detail | allowed | explicit owner 403 | forbidden |
| Authenticated admin with valid owner claim | allowed | allowed | allowed | explicit owner 403; no admin bypass | allowed, including self |
| Authenticated principal with missing/invalid NameIdentifier | depends on admin role only | depends on admin role only | explicit owner 403 | explicit owner 403 | depends on admin role only |

The source owner claim is ClaimTypes.NameIdentifier
(`http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier`). Do not use `sub`
as UUID: generated JWT sub contains email. Legacy also emits userId, but UsersController
does not fall back to it. Role checks use ClaimTypes.Role
(`http://schemas.microsoft.com/ws/2008/06/identity/claims/role`); tokens additionally contain
plain role. Token issuance normalizes role via trim/lowercase, but UserResponse does not.

Program.cs clears inbound/outbound JWT claim maps, validates signing key/lifetime with
zero skew, conditionally validates issuer/audience when configured, and requires a nonblank
unrevoked jti. Its token-validation event does not reload users or check current database
isActive/isEmailConfirmed/isDeleted. A soft delete or password update does not explicitly
revoke existing access tokens. Treat that as a security risk requiring a separate decision,
not permission to add silent checks or weaken authentication. Bearer 401/403 status is the
expected middleware behavior; exact challenge bodies/headers need captured fixtures.

### Exact service/controller envelope results

ApiResponse fields: success:boolean, message:string, data:T|null, errors:ApiErrorDetail[]|null,
traceId:string, timestamp:UTC DateTime (proposed Instant). ApiErrorDetail has field/code/message.

| Outcome | HTTP | success | message | data | errors | traceId |
|---|---:|---|---|---|---|---|
| list | 200 | true | empty string | array | null | empty string |
| detail found | 200 | true | empty string | UserResponse | null | empty string |
| detail missing/deleted | 200 | false | User not found. | null | null | empty string |
| PUT owner mismatch/claim invalid | 403 | false | Bạn chỉ được phép cập nhật thông tin của chính tài khoản mình. | null | null | HttpContext.TraceIdentifier |
| PUT owner allowed, user missing/deleted | 400 | false | User not found. | null | null | empty string |
| PUT success | 200 | true | User updated successfully. | UserResponse | null | empty string |
| DELETE success | 200 | true | User deleted successfully. | null | null | empty string |
| DELETE missing/deleted | 200 | false | User not found. | null | null | empty string |

Every wrapper is constructed with a current UTC timestamp. Do not insert a nonempty traceId
into service-created envelopes merely because infrastructure can generate one; the explicit
owner-rejection branch is different. Actual trace format and timestamp precision need capture.

No UsersController catch block or host custom exception handler is present. Repository
failures escape; do not assume they return ApiResponse, 409, or a sanitized 400. Standard
framework binding/validation for malformed JSON, wrong types, null/missing body and invalid
integer queries may short-circuit before controller code (typically 400), with a different
body shape. Wrong content types, invalid GUID routes, auth failures and unexpected 500
responses must be characterized against the pinned runtime/configuration. No invented error
payloads are accepted as fixtures. Spring defaults are not automatically compatible.

### Compatibility decisions still pending

- Exact physical users schema/collation/defaults versus the conflicting historical Db.sql.
- Runtime error/binding/route/serialization fixtures, including invalid and boundary inputs.
- Existing token validation/claim adapter and in-memory revocation interoperability before
  exposing protected Java routes; test-only principals cannot satisfy production parity.
- Legacy-compatible password encoding for PUT; no login/token/registration implementation
  is authorized by this analysis.

No endpoint is MIGRATED, DEPRECATED or REMOVED by MIG-004. MIG-001/002 example route/field
changes are expressly not adopted. Foundation code remains unchanged.

### Evidence closure update — 2026-09-18

No live User response was captured. The legacy host could not be executed safely because an
isolated database/configuration and .NET 10 runtime were unavailable; therefore all HTTP rows
above remain source-derived. No JSON example has been added or normalized.

Password-update format is now package-verified: Microsoft.AspNet.Identity.Core 2.2.4 emits a
standard-Base64 49-byte version-0 payload `{0x00, 16-byte random salt, 32-byte subkey}` using
PBKDF2-HMAC-SHA1 with 1,000 iterations. A synthetic package-produced vector was independently
verified on JDK 21. Strategy A—temporarily generate and verify this exact format—is recommended
for shared C#/Java table compatibility, pending implementation approval.

Claim evidence is source-complete: PUT consumes the literal ClaimTypes.NameIdentifier URI as a
GUID and has no admin bypass; list/detail/delete consume the ClaimTypes.Role URI value `admin`.
Java currently has no JWT consumer or revocation integration, so runtime authorization parity
is not established. User remains BLOCKED on actual-table evidence and live HTTP captures.

### Captured pinned-runtime contract — 2026-09-18

The disposable .NET 10 runtime produced 23 raw fixtures. GET list/detail and DELETE confirm the
route/status/envelope/pagination behavior above. Middleware 401 and role-based 403 responses have
empty bodies; 401 includes `WWW-Authenticate: Bearer`. List data is ordered by email. Zero page
values normalize to 1/1 and pageSize 101 clamps to 100. Delete mutates only soft-delete fields;
missing/deleted detail and repeated delete remain HTTP 200 failure wrappers.

PUT conflicts with the source-derived matrix: a real login-issued owner token was authenticated,
but even a route ID equal to its emitted NameIdentifier produced the explicit owner 403. Partial,
empty, empty-string and password requests caused no database mutation. Thus the captured public
behavior is currently “authenticated PUT always owner-forbidden” for tested login tokens; no
successful or missing-user service result is reachable. Do not label the source-intended 200/400
PUT rows verified. A compatibility decision is required before MIG-004 can be implemented.

Fixtures and provenance are under `src/test/resources/contracts/users/evidence/captured-2026-09-18/`.
MIG-004 remains PENDING; no Java endpoint is migrated.

### ClaimsPrincipal diagnosis and pending compatibility decision — 2026-09-19

The pinned runtime's owner 403 is a proven claim-mapping defect. The login token has both
`sub=owner@test.dev` and the literal NameIdentifier URI containing the owner UUID.
`JwtBearerOptions.MapInboundClaims=true` uses `JsonWebTokenHandler` 8.0.1 with its separate
73-entry default inbound map. It maps `sub` to the NameIdentifier URI, producing two claims of
that type with the email first. The controller's only `FindFirstValue(NameIdentifier)` resolves
the email, `Guid.TryParse` fails, and its explicit 403 branch runs. Clearing
`JwtSecurityTokenHandler` maps did not affect the active handler. A separately signed token with
email `sub` and UUID `nameid` reproduced the same transformation and 403.

- **Option A — strict observed compatibility:** keep owner PUT at 403. This preserves the capture
  but preserves an unusable profile/password update endpoint.
- **Option B — intentional legacy bug fix (recommended; not approved):** after full JWT
  validation, resolve UUID deterministically from `userId`, with a UUID-valued NameIdentifier
  fallback. Allow only route equality; wrong-owner and admin-other PUT remain 403. This documented
  deviation requires acceptance of proposed ADR-012 and tests separating legacy defect evidence
  from intended Java behavior.

Option B was explicitly approved on 2026-09-19 and implemented. Java validates the bearer token,
prefers a valid UUID `userId`, permits only matching-owner PUT, and denies wrong-owner and
admin-other updates. The legacy observed owner 403 remains preserved as defect evidence rather
than as the Java expected result. No registration, login, OTP, token issuance, refresh, logout or
full Authentication flow was implemented. MIG-004 is MIGRATED for dev scope; production cutover
remains blocked on the real users catalog and shared revocation interoperability.
## [MIG-005] Authentication compatibility slice — evidence/specification only

**Status:** IMPLEMENTED_NOT_VERIFIED

**Legacy source:** `a010fdc5894176596bb195e4fef66db2c09496f1`

**Evidence:** `src/test/resources/contracts/auth/evidence/2026-09-19/`

The verified legacy routes are `POST /api/Auth/register`, `/verify-otp`, `/login`,
`/refresh-token`, and authenticated `/logout`. Exact request fields, envelopes, messages, database
mutations, token format and failure statuses are specified in
`docs/AUTHENTICATION_MIGRATION_ANALYSIS.md`. No `/api/v1` prefix or renamed field is introduced.

Runtime capture verifies that login and refresh issue both access and rotated refresh tokens; only
an uppercase SHA-256 refresh hash is stored. Logout clears stored refresh state and writes JTI only
to a process-dev static dictionary. Revocation is lost on restart and is not visible across C#
and Java. Register accepts `{}` and OTP uses non-cryptographic randomness with no retry enforcement;
these require explicit compatibility/security decisions.

Authentication implementation readiness is **READY WITH DOCUMENTED LIMITATIONS**. MIG-005 is
implemented with the approved blank-input rejection, secure OTP RNG and bounded dev revocation,
but remains unverified until Java endpoints pass fixture parity.
Production cutover is BLOCKED by cross-runtime revocation and actual shared-catalog verification.

### Password-reset extension (new Java capability)

This capability is not present in the pinned legacy contract and is therefore an intentional,
documented extension rather than a migration-parity claim.

| Route | Request | Result |
|---|---|---|
| `POST /api/Auth/forgot-password` | `email` | Always returns the same 200 envelope after valid request validation, avoiding account enumeration. For an active, confirmed account, a cryptographically generated six-digit OTP is sent and expires after five minutes. |
| `POST /api/Auth/reset-password` | `email`, six-digit `otpCode`, `newPassword` (8–200 chars), `confirmPassword` | Returns 200 only when the OTP is valid, unexpired and both password fields match; otherwise returns the existing 400 failure envelope. Five incorrect OTP attempts consume the code. Success writes an ASP.NET Identity-compatible hash, consumes the OTP, and clears the stored refresh token. |

The existing `users.otp_code` and `users.otp_expired_at` fields are reused only for active,
confirmed accounts, so no schema migration or destructive database operation is introduced.

## [MIG-006] Remaining legacy controller surface — batch implementation

**Status:** IMPLEMENTED_NOT_VERIFIED

Java routes now exist for every pinned Ingredient, Recipe, Media, Pantry, Recommendation and Today
Menu/Cooking Log controller method. Paths and HTTP methods are listed in
`docs/ENDPOINT_COVERAGE.md`. The external AI implementation remains external; Java calls its six
legacy paths through an HTTP adapter. Media uses a Cloudinary port and environment configuration.
Catalog and media mutations require `SUPER_ADMIN`, `ADMIN`, or `MANAGER`. Pantry writes reject an
inactive ingredient, non-positive quantity, or blank/oversized unit. On pantry update, an omitted
`expiredAt` leaves the stored value unchanged while an explicit JSON `null` clears it.
No module covered by MIG-006 is parity-certified until legacy response capture, AI error-contract
tests, media compensation tests and complete multi-entity behavior tests pass.

### Ingredient batch-create extension

`POST /api/ingredients/batch` is a Java-only additive catalog-management route. It accepts
`{ "ingredients": [ ... ] }`, where each item uses the existing create-ingredient fields and
requires a nonblank `name` and `unit`. The operation is all-or-nothing: duplicate names in the
request or against an active canonical name/alias reject the whole list. On success, the normal
`ApiResponse` envelope returns an ordered list of `IngredientResponse` values, including `unit`
and `defaultQuantity` for frontend quantity forms. A missing positive `defaultQuantity` becomes
`100` for `g`/`ml` and `1` for other units. Existing single-create routes remain available.

## [MIG-007] Recommendation V2 personalization

**Status:** IMPLEMENTED_NOT_VERIFIED

Recommendation V2 is a new, versioned API. `POST /api/recommendations/v2/meals` derives Pantry
ingredients and the authenticated user profile on the server, then excludes declared recipe allergies
and ranks catalog recipes locally. It returns at most ten catalog recipes with a deterministic score,
Pantry-match ratio, matched/missing/expiring-soon ingredient names and short structured reasons.
It does not call the AI service. Profile values are controlled enums: `UserGoal`, `DietPreference`,
and `FoodAllergen`; unknown enum values are rejected at the profile boundary. The existing legacy
recommendation route remains unchanged.

The current score is intentionally transparent: each available required ingredient adds 30 points,
each missing required ingredient subtracts 10 points, and each matched ingredient expiring in the
next three days adds 20 points. Recipes with no required links use all of their linked ingredients.
Diet and nutrition targets are not yet hard filters because recipe suitability metadata is absent.
AI-backed conversational analysis is a separate future operation: it may receive only the already
ranked recipe IDs and their server-derived context, and may not introduce recipes outside that list.

## [MIG-008] Canonical-food Pantry previews

**Status:** IMPLEMENTED_NOT_VERIFIED

`POST /api/me/pantry/parse` and the image import analysis endpoints share one catalog-resolution
contract. Every item included in a preview has a non-null `ingredientId`, canonical database name,
database unit, and canonical Ingredient object. Provider names, units, and IDs are never trusted.
Missing or non-positive quantities are replaced by the Ingredient `defaultQuantity`. Image rows that
cannot be matched unambiguously to an active catalog record are omitted; their presence is conveyed
through a warning, not a fabricated ID. The text endpoint returns the same preview-item fields as
image analysis, wrapped in its existing `ApiResponse` list.

The text endpoint no longer calls Ollama or any text AI. It locally segments explicit input and
uses deterministic canonical-name/alias matching. Its route and preview response shape are unchanged.
Receipt and food-image endpoints continue to obtain only `ExtractedIngredient[]` from the external
Gemini service before reusing this exact backend pipeline.

### Monthly image-analysis allowance

The Java extension applies one authenticated-account allowance to receipt analysis, food-image
analysis and unified image analysis. The default is three requests per calendar month, resetting at
the start of a new month in the configured application zone. Exhaustion returns HTTP 429 and does not
call the AI provider. The current global default is configuration-owned; per-subscription allowances
are a future entitlement enhancement.

### Pantry batch-upsert extension

`POST /api/me/pantry/items/batch` is a Java-only additive authenticated route. It accepts
`{ "items": [ ... ] }`, where every item supplies an active `ingredientId`, positive `quantity`,
and nonblank `unit`, plus optional expiration, location and note fields. All rows are validated
before mutation; an invalid row or duplicate `ingredientId` rejects the entire request. A matching
existing pantry row for that user is updated with the same upsert semantics as the single-item route.
The normal `ApiResponse` envelope returns the saved rows in request order.

## [MIG-009] Food alias administration

**Status:** IMPLEMENTED_NOT_VERIFIED

**Breaking Change:** NO

**Date:** 2026-10-03

### New API

```http
GET    /api/ingredients/{ingredientId}/aliases
POST   /api/ingredients/{ingredientId}/aliases
DELETE /api/ingredients/{ingredientId}/aliases/{aliasId}
```

`POST` accepts `{ "aliasName": "..." }`. An alias is normalized before persistence and is rejected
when it conflicts with an active canonical Ingredient or active alias. Catalog mutation authorization
is the same existing `SUPER_ADMIN`, `ADMIN`, or `MANAGER` policy as Ingredient mutation. `GET` returns
the normal `ApiResponse` envelope with `id`, `ingredientId`, `aliasName`, and `normalizedAliasName`.

### Frontend Action Required

No existing client needs to change. Catalog administration can use these optional endpoints to maintain
the matching vocabulary.

## [MIG-010] Unified ingredient image analysis V2

**Status:** IMPLEMENTED_NOT_VERIFIED

**Breaking Change:** NO

**Date:** 2026-10-03

### New API

```http
POST /api/v2/ingredients/analyze-image
Content-Type: multipart/form-data
```

The request has one required field, `image` (JPEG, PNG, or WEBP, maximum 10 MB), and requires an
authenticated user. The response contains `imageType` (`RECEIPT`, `FOOD_IMAGE`, or `UNKNOWN`),
provider confidence, catalog-resolved preview `ingredients`, and warnings. `UNKNOWN` is HTTP 200 with
an empty ingredient list. The endpoint analyses only and never writes Pantry data. Existing V1 receipt
and food-image endpoints remain active and unchanged.

## [MIG-011] Structured user profile V2 for recommendations

**Status:** IMPLEMENTED_NOT_VERIFIED

**Breaking Change:** NO

**Date:** 2026-10-03

### New API

```http
GET /api/me/profile/v2
PUT /api/me/profile/v2
```

Both routes require the authenticated user and never accept a user identifier in the request.
`PUT` replaces the V2 profile with `birthDate`, `gender` (`MALE`, `FEMALE`, `OTHER`), `heightCm`,
`weightKg`, `activityLevel`, zero or more `goals`, `dietPreference`, and `allergies`. Enum values
outside the declared sets are rejected by JSON binding. `NO_ALLERGIES` is accepted only by itself;
combining it with a real allergen returns a client error.

The response retains the submitted profile and includes derived `bmi`, `bmr`, `tdee`, daily and
per-meal calorie/protein targets, `weightLossAllowed`, and a health warning where applicable.
The legacy `/api/users/{userId}/profile` API remains active and unchanged.

### Frontend Action Required

No existing client needs to migrate. A Recommendation V2 client may create or replace its structured
profile through this optional V2 endpoint before requesting meal suggestions.

## [MIG-012] Today-menu ingredient availability and shopping-list action

**Status:** IMPLEMENTED_NOT_VERIFIED

**Breaking Change:** NO

**Date:** 2026-10-09

### New API

```http
GET  /api/me/today-menu/items/{itemId}/ingredient-availability
POST /api/me/today-menu/items/{itemId}/missing-ingredients
```

Both routes require the authenticated owner. Availability returns `sufficient` and each required
ingredient's required, compatible available, and missing quantities after scaling for selected
servings. The POST operation recomputes shortages on the server and creates or refreshes pending
shopping-list rows; it never accepts a client-supplied quantity. Completion remains possible after
the client obtains confirmation, and only compatible quantities that actually exist are consumed.

### Frontend Action Required

Show shortages before completion and offer the shopping-list action. Keep the completion action
available after a user confirms that the meal was still cooked.

## [MIG-013] PayOS checkout completion and browser-to-mobile return

**Status:** IMPLEMENTED_NOT_VERIFIED

**Breaking Change:** NO

**Date:** 2026-10-09

### New API

```http
GET /payment/success
GET /payment/cancel
POST /api/payment/payos/webhook
```

These are public `text/html` browser landing pages used as the PayOS return and cancellation URLs.
They immediately attempt to open `zpantry://payment/success` or `zpantry://payment/cancel` and
also render an explicit fallback link for the installed Z-Pantry app. They ignore all redirect
parameters and do not change a payment or subscription state.

`POST /api/payment/payos/webhook` is the public provider callback. It accepts only payloads whose
PayOS HMAC-SHA256 signature validates against the configured checksum key. For an authenticated,
successful callback, a matching pending transaction is marked `PAID` and activates Z-Plus for 30
days. A verified unsuccessful callback marks a matching pending transaction `FAILED`. Replays and
terminal transactions are idempotently acknowledged. Invalid signatures receive HTTP 400 and never
change a transaction or subscription.

### Frontend Action Required

Configure the mobile `zpantry` URL scheme to route `payment/success` and `payment/cancel` to the
subscription screen, then reload the current subscription after the app returns. The server webhook
remains the only trusted path that may activate Z-Plus. Register the public HTTPS webhook URL in
the PayOS merchant dashboard before enabling checkout in an environment.
