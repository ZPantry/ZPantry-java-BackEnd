# Legacy ZPantry Backend

## 1. Baseline

Legacy implementation:

- ASP.NET Core
- .NET 10
- Entity Framework Core
- Npgsql/PostgreSQL
- pgvector
- JWT bearer authentication
- Cloudinary
- external AI service over HTTP
- Docker / Docker Compose

The repository is organized as a modular-monolith-style solution with a host project and two major modules.

```text
ZPantry-Backend/
├── AuthenticationModule/
├── ZPantryModule/
├── ZPantry_Backend/
├── Database/
├── Dockerfile
└── docker-compose.yml
```

---

## 2. Host Project

`ZPantry_Backend` is the ASP.NET Core host.

Its startup configuration is responsible for:

- PostgreSQL/EF Core registration;
- pgvector support;
- snake_case naming;
- repository/service dependency injection;
- AI HTTP client configuration;
- JWT authentication;
- authorization;
- controller registration;
- Swagger;
- CORS;
- schema/migration startup behavior;
- optional embedding backfill/demo initialization.

Legacy AI service default URL:

`http://localhost:8000`

The AI HTTP client is configured with an approximately 30-second timeout in the legacy host.

---

## 3. Legacy Projects

### AuthenticationModule

Contains more than authentication despite its name.

Important areas include:

```text
AuthenticationModule/
├── Contracts/
├── Controllers/
├── DTOs/
├── Repositories/
│   ├── Entities/
│   ├── Interfaces/
│   └── Implementations/
└── Services/
```

It contains the EF Core domain entities and DbContext used across the application.

### ZPantryModule

Contains feature controllers and services for the application domain.

Major capabilities include:

- ingredients;
- recipes;
- pantry;
- recommendations;
- today menu;
- media;
- AI integration;
- embedding backfill;
- vector search abstraction.

---

## 4. API Response Shape

The legacy backend uses a shared response wrapper.

Conceptually:

```text
ApiResponse<T>
├── Success
├── Message
├── Data
├── Errors
├── TraceId
└── Timestamp
```

Paged responses additionally include:

```text
PageIndex
PageSize
TotalItems
TotalPages
HasNextPage
HasPreviousPage
```

The Java migration should not casually replace this with Spring's default error/response format while API parity is required.

---

## 5. Authentication Behavior

Legacy authentication includes:

- register;
- OTP verification;
- login;
- access token;
- refresh token;
- refresh-token rotation;
- logout;
- JTI token blacklisting.

Important legacy details:

- JWT signing uses HS256;
- JWT validation includes issuer, audience, signing key, and lifetime;
- clock skew is effectively zero;
- token claims include user identity information and role;
- refresh tokens are random values returned to the client;
- only a SHA-256 hash of the refresh token is stored;
- logout clears the stored refresh token and blacklists the access token JTI.

The token blacklist implementation is currently in-memory, so revocation state is not durable across application restarts or multiple backend replicas.

---

## 6. Password Compatibility

Legacy password hashing uses the ASP.NET Identity password hasher through the older `Microsoft.AspNet.Identity` package.

This is a high-risk migration point.

Do not map it directly to Spring Security BCrypt and assume old users can still log in.

Exact legacy hash compatibility must be verified before final authentication cutover.

---

## 7. Data Access Behavior

The legacy domain uses UUID identifiers and a shared base entity containing audit/soft-delete fields.

Typical behavior:

- new entities receive a UUID;
- `created_at` is initialized;
- delete operations may set `is_deleted` rather than physically deleting rows;
- ordinary repository queries often filter `is_deleted = false`.

The Java migration should model soft deletion intentionally rather than relying on accidental repository behavior.

---

## 8. External Services

The backend integrates with:

- PostgreSQL + pgvector;
- AI service;
- Cloudinary;
- email/Gmail configuration.

See `INTEGRATIONS.md` for details.

---

## 9. Known Legacy Inconsistencies

The agent must not assume all legacy documentation is synchronized with executable code.

Known example:

- legacy changelog documentation describes Ingredient CRUD as removed;
- current executable/service code still contains Ingredient create/update/delete operations.

Therefore executable source and observed runtime behavior have higher priority than narrative changelogs.

Another mismatch exists between entity properties and the lightweight skeleton SQL around ingredient/recipe gradient fields. Verify the actual database before final JPA mapping.

---

## 10. Vector Search Status

A vector-search service abstraction exists in the legacy code, but the currently inspected implementation returns empty results.

Do not treat vector similarity search as a fully implemented legacy behavior merely because pgvector and embeddings exist.

Embedding generation/storage and vector search are separate migration concerns.
