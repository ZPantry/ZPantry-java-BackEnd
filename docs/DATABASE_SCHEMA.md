# ZPantry Database Schema Context

## Java migration ownership — 2026-09-19

`src/main/resources/db/migration/V1__legacy_schema_baseline.sql` is authoritative for empty/new
databases. It creates pgvector and the 13 mapped tables. Hibernate remains validation-only.
Default configuration keeps Flyway disabled; `application-fresh.properties` is the explicit opt-in.

The former test `foundation.sql` duplicated the schema and had drift, so it was removed. Every
Testcontainers persistence context now starts empty and runs V1 before Hibernate validation. Test
seed data is separate under `src/test/resources/db/seed/`.

EF evidence contains no foreign-key declarations and only the unique users-email index. V1 does
not invent constraints or vector indexes. See [Database Bootstrap](DATABASE_BOOTSTRAP.md).

## 1. Persistence Baseline

Database:

- PostgreSQL
- UUID primary keys
- snake_case naming
- pgvector extension
- embedding dimension: `vector(1536)`

The legacy EF Core DbContext exposes 13 principal entity sets:

1. users
2. ingredients
3. ingredient_aliases
4. recipes
5. recipe_ingredients
6. user_pantry_items
7. meal_recommendations
8. meal_recommendation_items
9. recommendation_feedbacks
10. media_assets
11. today_menu_items
12. cooking_logs
13. pantry_usage_logs

The Java migration must verify the **actual running database schema** before considering this document authoritative.

---

## 2. Shared Base Fields

Most legacy entities inherit a common base model:

```text
id          UUID
created_at  timestamp with time zone
created_by  UUID nullable
updated_at  timestamp with time zone nullable
updated_by  UUID nullable
deleted_at  timestamp with time zone nullable
deleted_by  UUID nullable
is_deleted  boolean
```

Java mapping should preserve audit and soft-delete semantics.

---

## 3. users

Important columns:

```text
id
created_at / audit fields
is_deleted
full_name
email
avatar_url
password_hashed
otp_code
otp_expired_at
otp_retry_count
is_email_confirmed
is_active
role
refresh_token_hash
refresh_token_expires_at
```

Important semantics:

- email is unique in the legacy skeleton;
- refresh token is stored as a hash;
- password value is a legacy ASP.NET Identity hash;
- soft deletion is used.

---

## 4. ingredients

Important columns:

```text
id
audit fields
name
normalized_name
category
unit
calories_per_unit
protein_per_unit
fat_per_unit
carb_per_unit
image_url
embedding vector(1536)
```

Legacy entity/DTO code also contains gradient-related properties (`GradientFrom`, `GradientTo`), while the inspected skeleton SQL does not contain corresponding columns.

**Action required:** verify the actual database and EF mapping before deciding whether these fields are persisted, computed, obsolete, or missing from the skeleton migration.

---

## 5. ingredient_aliases

```text
id
audit fields
ingredient_id
alias_name
normalized_alias_name
```

The lightweight skeleton SQL shows IDs but does not necessarily express the final relational constraints. Verify real FK/index definitions.

---

## 6. recipes

```text
id
audit fields
name
description
cooking_time_minutes
difficulty
serving_size
instruction_text
image_url
source_type
embedding vector(1536)
```

Legacy entity/DTO code also contains gradient-related properties not represented in the inspected skeleton SQL.

---

## 7. recipe_ingredients

```text
id
audit fields
recipe_id
ingredient_id
quantity
unit
is_required
note
```

This is the recipe-to-ingredient association carrying quantity/requirement metadata.

---

## 8. user_pantry_items

```text
id
audit fields
user_id
ingredient_id
quantity
unit
expired_at
storage_location
note
```

This table represents user-owned pantry inventory.

---

## 9. meal_recommendations

```text
id
audit fields
user_id
request_text
input_ingredient_text
recommendation_type
status
completed_at
```

Stores a recommendation request/session for a user.

---

## 10. meal_recommendation_items

```text
id
audit fields
meal_recommendation_id
recipe_id
match_score
missing_ingredient_count
missing_ingredient_names
reason
rank
```

Represents ranked recommendation items.

---

## 11. recommendation_feedbacks

```text
id
audit fields
user_id
meal_recommendation_id
recipe_id
rating
feedback_type
comment
```

Stores user feedback about recommendations.

---

## 12. media_assets

```text
id
audit fields
recipe_id nullable
ingredient_id nullable
public_id
url
secure_url
resource_type
format
width
height
```

Used to persist Cloudinary/media metadata.

---

## 13. today_menu_items

```text
id
audit fields
user_id
meal_id nullable
recipe_id nullable
meal_name
meal_type
serving_size
planned_date
status
note
cooked_at
image_url
image_public_id
```

Important:

- `planned_date` is a database date;
- status is represented as text in the legacy mapping.

---

## 14. cooking_logs

```text
id
audit fields
user_id
today_menu_item_id
meal_id nullable
recipe_id nullable
meal_name
image_url
image_public_id
cooked_at
rating
note
```

Created when a planned meal is completed.

---

## 15. pantry_usage_logs

```text
id
audit fields
user_id
today_menu_item_id
cooking_log_id
ingredient_id
ingredient_name
quantity_used
unit
action_type
warning
```

Captures pantry consumption effects associated with cooking.

---

## 16. Java Persistence Strategy

Initial Java migration rules:

- do not use Hibernate `create`, `create-drop`, or uncontrolled `update` against the legacy database;
- prefer schema validation during compatibility work;
- map UUIDs explicitly;
- map timestamps with timezone-awareness;
- preserve snake_case names;
- preserve numeric precision where the legacy schema uses values similar to `numeric(18,4)`;
- verify how pgvector is mapped in the selected Hibernate/driver stack;
- establish Flyway baseline only after the actual database has been inspected;
- keep EF migration history and Flyway history conceptually separate.

---

## 17. Schema Verification Checklist

Before marking database migration complete:

- inspect actual `information_schema` / PostgreSQL catalog;
- compare actual columns with EF entity mappings;
- compare actual constraints and indexes;
- verify pgvector extension/version;
- verify vector dimension;
- verify enum/string status values;
- verify nullable behavior;
- verify timestamp timezone semantics;
- verify soft-delete filtering assumptions;
- verify gradient field mismatch;
- verify actual FK constraints rather than assuming they exist from ID column names.

## Foundation persistence tests — 2026-09-16

Runtime schema policy is now explicit: Hibernate validate, SQL initialization never,
Flyway disabled, automatic baseline off, clean disabled. No Java-owned schema migration
has been introduced. Deployment must not override these with create/create-drop/update.
Supply SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD
outside version control; no connection defaults target a developer or legacy database.

src/test/resources/db/foundation.sql is a disposable Testcontainers init script, not a
Flyway migration or a representation of the 13 legacy tables. It enables vector and creates
foundation_probe with UUID, numeric(18,4), timestamptz, date and vector(1536). Hibernate
validates the test JPA scalar mapping; JDBC checks vector dimension/round-trip behavior.
Only the container-created DataSource is available to these tests. Real schema verification
and a future controlled Flyway baseline remain pending. On 2026-09-16 full Maven verify
passed against isolated PostgreSQL 16.14 / pgvector 0.8.2. All three persistence integration
tests passed; no legacy database was used or modified.



## User slice mapping analysis — 2026-09-16

**Evidence level:** exact source/migration-derived mapping, NOT a verified live schema.
The dev repository has no legacy pg_dump/schema-only export, catalog query result or
users-table integration fixture; its only SQL is the Foundation probe. Documentation
explicitly defers actual schema verification. No live legacy connection was used in this
analysis, and the passing Foundation PostgreSQL container is not legacy schema evidence.

Sources at a010fdc5894176596bb195e4fef66db2c09496f1:

- [BaseEntity](https://github.com/ZPantry/ZPantry-Backend/blob/a010fdc5894176596bb195e4fef66db2c09496f1/AuthenticationModule/Repositories/Entities/BaseEntity.cs)
- [User](https://github.com/ZPantry/ZPantry-Backend/blob/a010fdc5894176596bb195e4fef66db2c09496f1/AuthenticationModule/Repositories/Entities/User.cs)
- [ZpantryDbContext](https://github.com/ZPantry/ZPantry-Backend/blob/a010fdc5894176596bb195e4fef66db2c09496f1/AuthenticationModule/Repositories/Entities/ZpantryDbContext.cs)
- [InitialCreate](https://github.com/ZPantry/ZPantry-Backend/blob/a010fdc5894176596bb195e4fef66db2c09496f1/ZPantry_Backend/Migrations/20260703000100_InitialCreate.cs)
- [InitialSkeleton SQL](https://github.com/ZPantry/ZPantry-Backend/blob/a010fdc5894176596bb195e4fef66db2c09496f1/Database/Migrations/20260628000100_InitialSkeleton.sql)
- [Conflicting historical Db.sql](https://github.com/ZPantry/ZPantry-Backend/blob/a010fdc5894176596bb195e4fef66db2c09496f1/Database/Db.sql)

### Intended table and proposed Java mapping

EF explicitly maps users and uses snake_case naming. No schema is specified in ToTable or
in the skeleton CREATE TABLE; the actual schema/search_path must be established before
hardcoding public. Proposed User is a JPA class extending a @MappedSuperclass BaseEntity;
DTOs remain records. Use explicit column names and the following types/lengths. This is an
analysis proposal, not Java code or authorization to create/alter the table.

| Column | PostgreSQL type | Nullable | Skeleton DB default | Proposed Java field/type |
|---|---|---|---|---|
| id | uuid, primary key | no | none | BaseEntity.id: UUID |
| created_at | timestamptz | no | none | BaseEntity.createdAt: Instant |
| created_by | uuid | yes | none | BaseEntity.createdBy: UUID nullable |
| updated_at | timestamptz | yes | none | BaseEntity.updatedAt: Instant nullable |
| updated_by | uuid | yes | none | BaseEntity.updatedBy: UUID nullable |
| deleted_at | timestamptz | yes | none | BaseEntity.deletedAt: Instant nullable |
| deleted_by | uuid | yes | none | BaseEntity.deletedBy: UUID nullable |
| is_deleted | boolean | no | false | BaseEntity.isDeleted: boolean |
| full_name | varchar(150) | yes | none | fullName: String nullable |
| email | varchar(200), unique | no | none | email: String |
| avatar_url | varchar(500) | yes | none | avatarUrl: String nullable |
| password_hashed | varchar(500) | no | none | passwordHashed: String |
| otp_code | varchar(6) | yes | none | otpCode: String nullable |
| otp_expired_at | timestamptz | yes | none | otpExpiredAt: Instant nullable |
| otp_retry_count | integer | no | 0 | otpRetryCount: int |
| is_email_confirmed | boolean | no | false | isEmailConfirmed: boolean |
| is_active | boolean | no | false | isActive: boolean |
| role | varchar(50) | no | 'user' | role: String |
| refresh_token_hash | varchar(128) | yes | none | refreshTokenHash: String nullable |
| refresh_token_expires_at | timestamptz | yes | none | refreshTokenExpiresAt: Instant nullable |

Email uniqueness is unconditional in both EF mapping and skeleton SQL (not a partial index
for undeleted rows). No user foreign keys, navigation relationships, row-version or concurrency
token are configured in the inspected User mapping/skeleton. Audit UUIDs do not prove FKs.
Confirm actual constraints/index names, additional indexes, triggers, row-level security,
collation, timestamp precision, search_path and defaults from catalog evidence. Do not
invent relationships, an enum/check for role, @Version or a case-insensitive email type.

### UUID, audit and soft-delete behavior

BaseEntity's constructor assigns Guid.NewGuid() and DateTime.UtcNow; other nullable audit
fields remain null and IsDeleted defaults false. Proposed Java creation semantics, when
creation is eventually needed: application-generated random UUID and UTC Instant; no
Long/IDENTITY or database-generated ID requirement. Preserve existing UUIDs on loads and
updates. User CRUD slice has no create endpoint; do not add creation business behavior now.
Registration explicitly resets CreatedAt and leaves UpdatedAt null; demo bootstrap sets
UpdatedAt. The skeleton does not supply ID or created_at defaults.

UserRepository explicitly filters is_deleted=false on ID/email/refresh-hash lookup, page
and count queries. There is no global EF query filter. Do not rely on unfiltered inherited
JpaRepository findById/findAll/count/delete to reproduce this. Reads include inactive and
unconfirmed users. Email matching is ordinary database equality with no trim/lowercase;
actual collation behavior is unverified.

Profile update sets only provided non-null name/avatar, optionally replaces password hash,
and always sets updated_at, even for an empty/no-op request. It does not populate updated_by.
Delete loads a non-deleted user, sets is_deleted=true and deleted_at=UtcNow, then saves.
Delete does NOT explicitly change updated_at, created_at, deleted_by, updated_by, is_active,
OTP, role, password hash or refresh-token fields; it does not cascade into user-owned tables.
Repeated deletion becomes a missing-user result. Do not install a generic @PreUpdate or
JPA auditing hook that silently changes these fields on every save.

Repository Update uses EF Update plus SaveChanges, not a special password-only SQL query.
Each repository mutation saves immediately. Page retrieval and total count are separate
queries. Concurrency/lost-update behavior is not explicitly controlled; do not introduce
optimistic locking or transaction isolation changes as an undocumented compatibility fix.
Future tests must inspect non-target fields after updates and deletes, including auth fields.

### Conflicting schema artifacts and required evidence

InitialCreate.Up executes InitialSkeleton.sql; this is the only EF migration found in the
pinned tree. The script uses CREATE TABLE IF NOT EXISTS, so it does not reconcile an existing
table whose columns differ. InitialCreate.Down drops users; do not port or execute it.

Database/Db.sql instead creates public.users with quoted PascalCase columns such as Id,
FullName and Email, adds an Id default gen_random_uuid(), and omits avatar_url plus
created_by/updated_by/deleted_at/deleted_by/is_deleted. These are incompatible physical
layouts despite sharing the users table name. The host only repairs EF-history column names,
not these user columns. Neither old script nor narrative docs establish the actual live table.

Before finalizing the mapping, obtain a schema-only dump or read-only catalog evidence for
users: qualified table name; columns/types/lengths/nullability/defaults; PK/unique/FK/check
constraints and index definitions; collation and timestamp precision; triggers/RLS;
EF-history state. A safe fixture reflecting that evidence must then run in Testcontainers
with Hibernate validate. Do not apply either legacy SQL file to an existing database.

The API contract and exposure whitelist live only in API_MIGRATION.md MIG-004. Password
hash, OTP and refresh state must remain persistence-only even though the legacy UserResponse
explicitly includes isActive, isEmailConfirmed and role. No schema migration is proposed.

### Evidence-closure probe — 2026-09-18

The actual-table gate remains **BLOCKED**. A dev PostgreSQL 17 Windows service was detected,
but no connection environment variable or PostgreSQL password file was available. A no-prompt
`psql -w -h devhost -U postgres -d postgres` attempt failed with `no password supplied`
before any session or SQL statement. Consequently no database identity, read-only transaction,
catalog row, column, constraint, index, default, collation, trigger or RLS evidence was collected.
No database was modified.

Do not reinterpret the running dev service as the legacy/development ZPantry database. The
20-column EF/skeleton mapping above remains provisional, and the conflicting quoted-PascalCase
Db.sql remains unresolved. Evidence closure requires an explicitly supplied read-only connection
or a schema-only/catalog export from the actual environment. The query session must first confirm
database/server identity and `transaction_read_only` before querying `pg_catalog` and
`information_schema`; no migration, extension creation or application startup is permitted.

### Disposable pinned-runtime evidence — 2026-09-18

The pinned .NET 10 backend successfully applied `20260703000100_InitialCreate` to a private
`pgvector/pgvector:0.8.2-pg16` database (PostgreSQL 16.14). The resulting `public.users` catalog
matches the 20-column snake_case table above exactly, including ordinal positions, nullability,
lengths, timestamptz(6), defaults, non-identity UUID and indexes. Evidence is stored in
`src/test/resources/contracts/users/evidence/captured-2026-09-18/disposable-users-catalog.json`.

Observed constraints/indexes are only `users_pkey` on id and unconditional `users_email_key`
on email. There are no User foreign keys, additional indexes, identity columns or database UUID/
created_at defaults. Legacy bootstrap inserted application-generated UUIDs and timestamps.
The table defaults are false for is_deleted/is_email_confirmed/is_active, 0 for otp_retry_count,
and `user` for role. All audit/security columns in the proposed mapping exist.

Admin DELETE changed only `is_deleted` from false to true and `deleted_at` from null to the
current UTC timestamp. It left updated_at, all actor UUIDs, active/confirmed state, role,
password hash, OTP fields and refresh-token fields unchanged. Subsequent repository-backed reads
excluded the row. This validates soft-delete behavior in the disposable migrated schema.

Disposable migration mapping is **VERIFIED**, but the actual users-table evidence gate remains
**BLOCKED**. The official migration, EF mapping and disposable runtime agree, so this provides a
strong implementation candidate behind Hibernate validation. The shared legacy/development
database remains unverified and production cutover must remain blocked until a read-only
catalog/export confirms it is not the incompatible Db.sql layout.
## Implemented User mapping — 2026-09-19

`UserEntity` now maps the disposable legacy catalog exactly: UUID primary key; timestamptz audit,
OTP and refresh expiry values as `Instant`; all documented varchar lengths/nullability; primitive
boolean/int fields; and the 20 snake_case columns. `BaseEntity` owns the eight shared UUID/audit/
soft-delete columns. IDs are generated in Java when absent; no identity/sequence strategy exists.

Hibernate remains `ddl-auto=validate`; Flyway and SQL initialization remain disabled normally.
The isolated Testcontainers init script creates the evidence-backed table only inside its private
PostgreSQL 16 database. No application migration or legacy/shared schema mutation was added.
The actual shared catalog comparison is still required before production cutover.
## Java batch mappings — 2026-09-19

JPA mappings now cover users, ingredients, ingredient_aliases, recipes, recipe_ingredients,
user_pantry_items, meal_recommendations, meal_recommendation_items, recommendation_feedbacks,
media_assets, today_menu_items, cooking_logs and pantry_usage_logs. IDs remain UUID and all entities
inherit audit/soft-delete columns. Ingredient/Recipe embeddings map to `vector(1536)`. These mappings
passed Hibernate validation only against the disposable Testcontainers schema; the shared legacy
catalog remains unverified and no production migration was run.
