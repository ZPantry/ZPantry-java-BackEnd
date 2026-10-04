# Database Bootstrap

## Clean or new database

For normal IDE/Maven development with Docker available, run:

```powershell
.\mvnw.cmd spring-boot:run
```

With no explicit profile, the safe `dev` default starts the PostgreSQL 16/pgvector service from
`compose.yaml` on port `15432`, applies Flyway V1 and validates Hibernate. All database values are
synthetic and dev-only. The named Docker volume preserves dev data between runs.

The Java-owned Flyway chain is approved only for an empty PostgreSQL 16 database with pgvector
available. Supply the datasource through the environment and activate the explicit `fresh` profile:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://devhost:5432/zpantry_dev'
$env:SPRING_DATASOURCE_USERNAME='zpantry_dev'
$env:SPRING_DATASOURCE_PASSWORD='<dev-only-password>'
$env:SPRING_PROFILES_ACTIVE='fresh'
.\mvnw.cmd spring-boot:run
```

Flyway applies `V1__legacy_schema_baseline.sql`, then Hibernate validates the mappings with
`ddl-auto=validate`. Spring SQL initialization remains disabled. The database role must be able to
run `CREATE EXTENSION vector`, or an administrator must install the extension first.

Tests apply `src/test/resources/db/seed/test-data.sql` explicitly. Spring never loads it
automatically. For an empty Java-owned database, Flyway V7 seeds 15 Vietnamese ingredients, five
recipes and their required-ingredient links. The migration inserts data only and is idempotent if
replayed manually. Do not enable this Flyway chain against an existing/shared legacy database
without explicit approval.

## Existing legacy database

Do not activate `fresh` against an EF-created/shared database. Obtain a read-only catalog, compare
every table, column, constraint, index, extension and migration-history record with V1, then approve
an explicit Flyway adoption version. `baseline-on-migrate` remains false. This work did not connect
to or modify an existing database.
