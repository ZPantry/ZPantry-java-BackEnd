# Legacy contract fixtures

These fixtures supplement API_MIGRATION.md; they are not a replacement API specification.

Feature directories: auth, users, ingredients, recipes, pantry, recommendations, today-menu.
No endpoint response payloads have been captured yet. Do not turn migration-guide examples into fixtures.

For each future scenario, check in a provenance document with the exact legacy commit,
source paths or OpenAPI hash, capture method/date, route/method/query, HTTP status,
content type, auth/role/ownership context, sanitized fixture-data setup and expected DB side effects.
Label evidence as source-derived, OpenAPI-derived or runtime-captured. Source-derived evidence
is not runtime parity. Never commit tokens, password hashes, OTPs, secrets or personal data.

Store request and response bodies only when supported by that evidence. Compare parsed JSON
with exact keys, null versus absent values, numeric types and array order. Compare status and
headers separately. Assert timestamp format and UTC semantics; compare trace IDs for nonempty
request correlation. Normalize only documented nondeterministic paths (not entire error/data
objects). Run both services against separate disposable fixtures, never a shared writable legacy DB.
Record known discrepancies explicitly; do not approve a new snapshot just to make a test pass.

## Common envelope evidence

Source: ZPantry/ZPantry-Backend commit a010fdc5894176596bb195e4fef66db2c09496f1:
- AuthenticationModule/Contracts/Common/ApiResponse.cs
- AuthenticationModule/Contracts/Common/ApiErrorDetail.cs
- AuthenticationModule/Contracts/Common/PagedResponse.cs
- ZPantry_Backend/Program.cs (AddControllers without JSON naming/null overrides)

The Java common records preserve the flat envelope and pagination calculation derived from
these types. ResponseContractTest uses synthetic scalar inputs to test serialization, not a
fabricated captured endpoint response. Wire naming/null behavior and timestamp precision still
need runtime captures before a feature can claim parity.

Global exception mapping remains deferred: AuthController uses endpoint-specific statuses and
sometimes exposes exception messages; automatic ASP.NET validation may use another shape.
Do not install a catch-all handler or force every error into this envelope without fixtures.
The traceId field is available, but generation/propagation is deferred until a real HTTP adapter
requires it. No new request-ID header contract is introduced by the foundation.
