# User evidence closure

`captured-2026-09-18/` contains raw evidence from the pinned ASP.NET Core/.NET 10 backend at
commit `a010fdc5894176596bb195e4fef66db2c09496f1`. It ran in Docker against a private disposable
PostgreSQL 16/pgvector database containing three synthetic users. No real database, account,
email service, Cloudinary service or production credential was used.

For each HTTP case, `*.body.json` is the exact response body emitted by Kestrel and
`*.headers.txt` is the captured response header block. Empty body files for middleware 401/403
responses are intentional. `manifest.json` records requests, actors, statuses and provenance;
it does not persist bearer or refresh tokens. Before/after row captures record mutations.
`disposable-users-catalog.json` records the schema created by the official legacy migration.

The capture exposed a material source/runtime conflict: login-issued owner tokens authenticate,
but every matching-owner PUT is rejected with the explicit owner 403 before UserService runs.
Consequently no successful profile/password PUT fixture exists, and the source-defined mutation
semantics remain unobserved through HTTP. Do not fabricate that missing success case or treat the
rejected password fixture as a password update.

The actual shared legacy/development database remains unavailable. The disposable catalog proves
what the pinned migration creates, not what production currently contains.

`claims-diagnosis-2026-09-19/` closes the PUT conflict. The login JWT contains email `sub` and a
UUID-valued literal NameIdentifier. The active `JsonWebTokenHandler` maps `sub` into a first
email-valued NameIdentifier, preserves the UUID-valued claim second, and the controller's
`FindFirstValue` selects email. UUID parsing therefore fails. The directory records the same
mechanism for an equivalent independently signed token. Tokens, signing key and JTI values are
not persisted; instrumentation existed only in the disposable legacy copy.
