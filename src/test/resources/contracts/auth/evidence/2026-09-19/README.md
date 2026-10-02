# Legacy Authentication Runtime Evidence — 2026-09-19

This directory contains redacted observations from the pinned legacy commit
`a010fdc5894176596bb195e4fef66db2c09496f1` running on .NET 10 against a disposable
PostgreSQL 16/pgvector container. All actors use `@test.dev`; the database and Docker network
were isolated from developer and shared environments.

The disposable source copy intercepted `EmailService.SendEmailAsync` at the provider boundary.
It returned without delivery, except for `delivery-fail@test.dev`, where it threw a synthetic
failure. Registration, persistence, OTP, hashing and controller behavior were otherwise unchanged.
The instrumentation was outside this repository and is not production code.

Each JSON file records the endpoint, redacted request, status, content type and response body.
Database snapshots redact password hashes, OTP values and refresh-token hashes while retaining
presence, timestamps and state columns. `token-metadata.json` records decoded claim names and
non-secret format/lifetime metadata. No usable password, access token, refresh token, JWT key,
email credential or database credential is retained.

The capture is evidence for compatibility tests, not a normative redesign. Dynamic UUIDs,
timestamps, trace IDs and token values must be matched structurally. See
`docs/AUTHENTICATION_MIGRATION_ANALYSIS.md` for the source trace, mutation matrix and decisions.

