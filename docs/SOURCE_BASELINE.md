# Legacy Source Baseline

## Repository

Legacy repository:

```text
ZPantry/ZPantry-Backend
```

GitHub clone URL:

```text
https://github.com/ZPantry/ZPantry-Backend.git
```

## Inspected Baseline

At context-pack creation time:

```text
branch: main
commit: a010fdc5894176596bb195e4fef66db2c09496f1
commit date: 2026-08-09
commit message: Refactor Dockerfile for improved clarity and structure
```

The repository main branch was rechecked on 2026-09-16 and still resolved to that commit.

## Important Inspected Source Areas

```text
ZPantry_Backend/Program.cs
ZPantry_Backend/ZPantry_Backend.csproj
ZPantry_Backend/Migrations/
AuthenticationModule/AuthenticationModule.csproj
AuthenticationModule/Controllers/
AuthenticationModule/DTOs/
AuthenticationModule/Repositories/
AuthenticationModule/Services/
AuthenticationModule/authenticationconfig.json
ZPantryModule/ZPantryModule.csproj
ZPantryModule/Controllers/
ZPantryModule/DTOs/
ZPantryModule/Services/
Database/
Dockerfile
docker-compose.yml
.env.example
CHANGELOG_SUMMARY.md
```

## Important Rule

This baseline is historical context, not a permanent truth.

If the legacy repository moves beyond the commit above:

1. compare the new legacy commit with this baseline;
2. identify changed modules/contracts/schema;
3. update the relevant docs in this Java repository;
4. update this file with the new verified baseline.

Do not assume this documentation automatically tracks upstream changes.
