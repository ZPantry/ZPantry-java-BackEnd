# ZPantry Java Target Architecture

## Schema ownership for new databases

Flyway owns schema creation for explicitly selected Java-owned empty databases. Hibernate owns
mapping validation (`ddl-auto=validate`). Existing/unverified EF databases must keep Flyway
disabled. V7 seeds only the initial ingredient/recipe catalog for empty Java-owned databases.

Developer launches without an explicitly active profile use the `dev` default and repository
`compose.yaml`. Deployments must activate their environment profile and provide their datasource.

## 1. Target Stack

Current migration baseline:

- Java 21
- Spring Boot 4.0.8
- Maven
- Spring Web
- Spring Data JPA
- Spring Security
- Bean Validation
- PostgreSQL
- pgvector integration
- Flyway
- OpenAPI documentation
- Cloudinary integration
- HTTP client for the external AI service

Do not treat this list as permission to upgrade dependencies arbitrarily during feature migration.

---

## 2. Architecture Style

Use a **modular monolith with package-by-feature**.

Recommended logical structure:

```text
com.zpantry
├── common
│   ├── api
│   ├── error
│   ├── security
│   └── persistence
├── auth
├── user
├── ingredient
├── recipe
├── pantry
├── recommendation
├── todaymenu
├── media
└── integration
    └── ai
```

Each feature may contain its own:

```text
controller
service
repository
entity/domain
dto
mapper
```

Do not create a global `controllers/`, `services/`, `repositories/` structure that makes feature boundaries harder to understand unless the existing Java codebase has already intentionally chosen that architecture.

---

## 3. Dependency Direction

Preferred direction:

```text
Controller
   ↓
Application/Service
   ↓
Repository / Integration Port
   ↓
Database / External Client
```

HTTP controllers should not contain persistence logic.

JPA entities should not become API response DTOs by default.

External AI/Cloudinary details should not leak directly into domain-facing controllers.

---

## 4. Cross-Cutting Concerns

Keep shared infrastructure in focused cross-cutting packages:

- API response wrapper;
- pagination metadata;
- global exception mapping;
- security filter/configuration;
- request tracing;
- common persistence/auditing support.

Do not move feature-specific business logic into `common`.

---

## 5. API Compatibility Boundary

During the migration phase, controllers are compatibility adapters.

They may use internal Java models that are cleaner than the C# implementation, but the externally visible contract should remain compatible with `API_MIGRATION.md` unless an intentional API version change is approved.

This allows:

```text
legacy API contract
        ↓
Spring controller/DTO adapter
        ↓
clean internal Java application model
```

---

## 6. Persistence Boundary

The existing PostgreSQL schema is a compatibility boundary.

Initially:

- JPA maps existing tables;
- Hibernate validates;
- Flyway is introduced carefully;
- schema redesign is deferred.

Do not combine a language migration and a broad database redesign in the same step unless explicitly approved.

---

## 7. Authentication Boundary

Authentication should be split conceptually into:

```text
credential verification
token issuance
refresh-token lifecycle
authorization identity
token revocation
```

Legacy password verification compatibility must be handled separately from the future preferred Java password encoder.

A safe transition may be:

```text
existing legacy hash
      ↓
legacy verifier
      ↓ successful login
new Java password encoding
      ↓
persist upgraded hash
```

Only implement this after verifying the exact legacy hash format and after deciding whether existing-account continuity is required.

---

## 8. External AI Boundary

The AI service remains an external service.

The Java backend should recreate the HTTP integration contract; it should not absorb/rewrite the AI service as part of the backend language migration.

Use an integration/client abstraction so application services do not depend directly on low-level HTTP details.

---

## 9. Vector Search

Treat these separately:

1. embedding generation;
2. embedding storage;
3. vector similarity query;
4. ranking/business use of similarity.

The legacy vector-search service is currently incomplete/stubbed.

Therefore the Java migration should first preserve existing observable behavior and only implement real vector search as a separately approved enhancement.

---

## 10. Migration Unit

Prefer vertical slices over mass translation.

Example:

```text
User DTO
→ User entity mapping
→ repository
→ service
→ controller
→ authorization
→ tests
→ documentation
```

Complete and verify one slice before spreading partial implementations across every module.

---

## 11. Definition of Done for a Migrated Module

A module is not complete until:

- relevant legacy behavior has been inspected;
- API behavior is implemented;
- persistence behavior is compatible;
- authorization behavior is compatible;
- relevant unit/integration tests pass;
- API migration status is updated;
- migration status is updated;
- new risks/decisions are documented.

## Foundation implementation — 2026-09-16

The composition root now lives at com.zpantry.ZPantryBackendApplication. Future feature
packages under com.zpantry are inside component, repository and entity scanning without
additional scan overrides. common.api contains source-derived ApiResponse, ApiErrorDetail
and flat PagedResponse records. No controller or business module has been introduced.

Bean Validation is an explicit dependency. Global exception mapping and trace-ID generation
were audited and deferred: endpoint-specific status/error behavior and automatic ASP.NET
validation need verified fixtures, and no HTTP adapter currently consumes request tracing.
The response record carries traceId without inventing a new header contract. No catch-all
exception handler exposes internals or silently replaces legacy errors.

Normal persistence uses Hibernate validate, SQL initialization never, Open Session in View
disabled, and Flyway disabled with baseline-on-migrate=false and clean-disabled=true.
Datasource credentials must be supplied externally through Spring datasource settings.
These are repository defaults; deployment overrides must retain the same safety policy.
No migration scripts or runtime database/extension creation are included.

Spring AI's generic vector-store starter/BOM was removed from this unused scaffold. It is
not a mapper for the existing embedding columns. pgvector is exercised only by disposable
integration tests; future entity-vector mapping and external AI clients require their own slices.

Test-only FoundationProbe/IsolatedPostgres are not shipped in the application jar. Their
SQL creates one disposable probe table and the vector extension only inside Testcontainers.
This is infrastructure testing, not the legacy database schema or a new business entity.
## Implemented User identity boundary — 2026-09-19

The User feature contains a bounded legacy-token consumer: `LegacyClaimConverter` produces an
immutable `AuthenticatedUser`; `LegacyJwtAuthenticationConverter` maps normalized roles once;
and `TokenRevocationChecker` is the only dependency on future revocation infrastructure. User
services receive the immutable identity and never inspect raw JWT claims.

JWT support is configuration-gated. Disabled configuration denies every `/api/users` request.
Enabled configuration requires an HS256 secret of at least 32 bytes plus a revocation-checker
bean, validates zero-skew lifetime and optional issuer/audience, and rejects missing/revoked JTI.
There is deliberately no production allow-all revocation implementation. Full Authentication
remains a later module.
Authentication implementation will depend on the existing User repository, `LegacyPasswordHasher`,
`AuthenticatedUser` and claim converter. Authentication owns its controller/orchestration, token
issuance, refresh/OTP services, email port and revocation writes. Dependency direction is
authentication → user/common; User does not depend on Authentication. Mixed-runtime deployment
requires either a shared JTI revocation store or single-runtime bearer routing with token drain.

## Batch feature architecture — 2026-09-19

Production packages now follow feature ownership: `authentication`, `ingredient`, `recipe`,
`media`, `pantry`, `recommendation`, `todaymenu`, and `integration.ai`. Authentication reuses User
identity, repository and password compatibility. Media and AI calls are ports with environment-only
adapters. Ingredient and Recipe use Hibernate vector mappings for `vector(1536)`. Normal persistence
still validates schema and never creates or updates it.
## Pantry Image Import and Recommendation V2

```text
Client image -> Java Pantry Import -> AI service -> Gemini -> preview -> user confirm -> Pantry
JWT -> Java Recommendation V2 -> Profile + Pantry -> allergen filter -> AI service -> ranked recipes
```

Java never stores a Gemini key and never writes AI extraction directly to Pantry. The AI service owns
provider calls. Recommendation V2 derives user context from the validated JWT and server-side data;
the client supplies only an optional result limit.

## Canonical food resolution pipeline — 2026-10-03

`ingredients` is the canonical-food catalog; `ingredient_aliases` is a controlled vocabulary that
maps alternate product, brand, language, or colloquial labels to one canonical food. The backend owns
the following pipeline:

```text
text local extraction OR Gemini image extraction
        -> ExtractedIngredient[]
        -> FoodMatchingService (canonical exact -> alias exact -> unique phrase match)
        -> ResolvedIngredient
        -> PantryIngredientPipeline preview
        -> explicit user confirmation
        -> PantryService upsert
```

No category search, fuzzy matching, provider-supplied ID, or provider-supplied unit can select a
catalog ingredient. Ambiguous or unknown data produces no preview row. Text parsing does not call an
AI provider. Gemini remains isolated to receipt/food-photo extraction through the AI service.

## Structured profile V2 — 2026-10-03

The additive `/api/me/profile/v2` boundary belongs to the User feature and shares the existing
`user_profiles` row without changing the legacy profile route. The controller obtains the UUID only
from the validated JWT, the service normalizes controlled enum values, and
`ProfileMetricsCalculator` deterministically derives BMR, TDEE and targets. Recommendation V2 will
read this server-owned profile; clients do not submit derived targets or a user ID to recommendation.
