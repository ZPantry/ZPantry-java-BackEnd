# ZPantry Java Backend — Agent Instructions

> This file is the primary instruction file for coding agents working in the Java migration repository.
> Keep it small, current, and operational. Project facts belong in `docs/`, not here.

## 1. Mission

Migrate the existing **ZPantry ASP.NET Core/.NET 10 backend** to Java while preserving observable behavior unless a change is explicitly approved and documented.

Target baseline:

- Java 21
- Spring Boot 4.0.8
- Maven
- PostgreSQL 16
- pgvector
- Flyway
- Modular monolith / package-by-feature

The migration goal is:

**behavioral compatibility first → controlled redesign second**

Do not translate C# files line-by-line. Migrate capabilities, contracts, data semantics, and side effects.

---

## 2. Existing Java Skills

This repository already contains reusable Java/Spring skills under:

`.agents/skills/`

Use the relevant skills for implementation details.

Expected skill areas include:

- Java object-oriented design
- Java type design
- Java secure coding
- Java unit testing
- Spring Boot core
- Spring Boot REST
- Spring Boot validation

Do not duplicate generic Java/Spring guidance inside project documentation unless it is a ZPantry-specific architectural decision.

---

## 3. Required Reading Order

Before making a substantial change, read the documents relevant to the task.

Start with:

1. `AGENTS.md`
2. `PROJECT_CONVENTIONS.md`
3. existing `API_MIGRATION.md`
4. `docs/PROJECT_CONTEXT.md`
5. `docs/LEGACY_SYSTEM.md`
6. `docs/MIGRATION_STATUS.md`

Then read as required:

- Persistence work → `docs/DATABASE_SCHEMA.md`
- Architecture work → `docs/ARCHITECTURE.md`
- External services → `docs/INTEGRATIONS.md`
- Security/authentication → `docs/KNOWN_RISKS.md`
- Testing/parity work → `docs/TEST_STRATEGY.md`
- Architecture-changing decisions → `docs/ARCHITECTURE_DECISIONS.md`
- Legacy baseline verification → `docs/SOURCE_BASELINE.md`

`API_MIGRATION.md` already exists in the repository and is the primary API migration document.

**Do not create a second competing API specification.**

---

## 4. Authority and Source-of-Truth

There are two different authority chains.

### 4.1 Behavioral compatibility

When deciding **what behavior must be preserved**, use:

1. Executable legacy C# implementation
2. Actual PostgreSQL schema used by the running environment
3. Observed legacy API behavior / exported OpenAPI contract
4. Existing `API_MIGRATION.md`
5. EF/database migration files
6. Other legacy documentation and changelogs

### 4.2 Java implementation style

When deciding **how compatible behavior should be implemented in Java**, use:

1. `PROJECT_CONVENTIONS.md`
2. `AGENTS.md`
3. Existing Java project architecture
4. `.agents/skills/`
5. General best practices

Project conventions must not silently override legacy behavior or public contracts.

Do not silently resolve contradictions.

When a contradiction is discovered:

1. record it in `docs/KNOWN_RISKS.md` or the relevant document;
2. state which behavior the Java implementation follows;
3. create/update an ADR if the resolution changes architecture or compatibility.

---

## 5. Migration Rules

Preserve legacy observable behavior unless an intentional redesign is approved.

Compatibility includes:

- endpoint paths;
- HTTP methods;
- request field names and meanings;
- response wrapper structure;
- HTTP status behavior;
- pagination semantics;
- authentication behavior;
- authorization behavior;
- JWT claim semantics;
- refresh-token behavior;
- soft-delete behavior;
- database semantics;
- external-service calls;
- important business side effects.

Do not redesign an API merely because a different Spring convention is cleaner.

Do not implement functionality that the legacy system only intended but never actually implemented unless that is an explicit new feature.

---

## 6. Documentation Is Part of the Code

Whenever a task changes any of the following, update documentation in the **same task**:

- architecture;
- module boundaries;
- API behavior;
- database schema or mappings;
- authentication or authorization;
- external integrations;
- configuration;
- dependencies;
- migration status;
- known limitations;
- compatibility decisions.

Update matrix:

| Change | Required document |
|---|---|
| Migration progress | `docs/MIGRATION_STATUS.md` |
| Architecture/module boundary | `docs/ARCHITECTURE.md` and usually `docs/ARCHITECTURE_DECISIONS.md` |
| Persistence/schema | `docs/DATABASE_SCHEMA.md` |
| API contract/status | existing `API_MIGRATION.md` |
| External integration | `docs/INTEGRATIONS.md` |
| Newly found risk/inconsistency | `docs/KNOWN_RISKS.md` |
| Significant completed work | `docs/CHANGELOG.md` |

A task that changes behavior or architecture but leaves these documents stale is incomplete.

---

## 7. End-of-Task Procedure

Before declaring a substantial task complete:

1. inspect the relevant legacy implementation;
2. implement the smallest compatible vertical slice;
3. build the Java project;
4. run relevant tests;
5. verify API compatibility;
6. verify database compatibility;
7. review changed files for accidental redesign;
8. update migration status;
9. update relevant project documentation;
10. record unresolved issues;
11. update `docs/CHANGELOG.md`.

**Compiling successfully is not evidence that migration is correct.**

---

## 8. Security Rules

Never copy or commit secrets from the legacy repository.

Never commit:

- Gmail credentials;
- JWT signing secrets;
- database passwords;
- Cloudinary API secrets;
- external API keys;
- refresh tokens;
- private environment files.

Use environment variables or secret-management mechanisms.

The legacy repository has a known history of sensitive configuration being committed. Treat all such values as compromised until rotated.

---

## 9. Database Safety

The Java migration must not casually recreate or mutate the existing schema.

Initial persistence policy:

- map the existing database first;
- prefer Hibernate schema validation rather than auto-create/update;
- establish a Flyway baseline before Java-owned migrations;
- do not reuse EF Core migration-history semantics as Flyway history;
- verify the real database before trusting old SQL scripts;
- preserve UUID and soft-delete semantics.

Any destructive schema operation requires explicit human approval.

---

## 10. Authentication Safety

The legacy application uses ASP.NET Identity password hashing behavior.

Do **not** assume existing hashes are BCrypt.

Before implementing login compatibility:

1. verify the exact legacy hash format;
2. implement/test a legacy verifier if password continuity is required;
3. optionally rehash with the new Java strategy after successful legacy verification;
4. document the transition.

Do not invalidate all existing passwords accidentally.

---

## 11. Preferred Migration Sequence

Default sequence:

Foundation  
→ User  
→ Authentication  
→ Ingredient  
→ Recipe  
→ Media  
→ Pantry  
→ Recommendation  
→ Today Menu  
→ AI integration  
→ pgvector/vector search  
→ deployment cutover

This sequence is a default, not a substitute for `docs/MIGRATION_STATUS.md`.

---

## 12. Working Principle

For each feature:

**legacy behavior → understand → specify → implement in Java → test parity → document**

Do not start with:

> “Which Java class is equivalent to this C# class?”

Start with:

> “What behavior does this legacy capability provide, what data does it touch, and what contract must remain stable?”
