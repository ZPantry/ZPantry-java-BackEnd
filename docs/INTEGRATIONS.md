# ZPantry External Integrations

## 1. AI Service

The backend calls a separate AI service.

Legacy default service URL:

```text
http://devhost:8000
```

Known legacy paths:

```text
ai/recommend-meals
ai/suggest-missing-ingredients
ai/check-meal-ingredients
ai/check-today-menu-completion
ai/embed-ingredient
ai/embed-recipe
```

The Java migration should preserve request/response behavior rather than inventing new AI endpoints.

The client should explicitly handle:

- timeout;
- connection errors;
- non-success HTTP responses;
- malformed JSON;
- null/incomplete AI data.

Do not convert an AI failure into a successful empty recommendation without confirming legacy behavior.

---

## 2. Cloudinary

Cloudinary is used for media/image storage.

Legacy behavior includes:

- upload;
- delete by public ID;
- media metadata persistence;
- association with recipe and/or ingredient where applicable;
- image upload during today-menu completion.

Java configuration must use environment variables or externalized configuration.

Do not copy legacy Cloudinary secrets.

---

## 3. Email / OTP

Registration includes email-based OTP verification.

Legacy behavior includes a short numeric OTP with an expiration window.

Before final Java migration, verify:

- OTP length;
- expiration;
- retry behavior;
- when account activation/email confirmation changes;
- exact email provider/configuration;
- whether email failure rolls back registration or produces a partial account.

Do not place Gmail/app passwords in source control.

---

## 4. PostgreSQL / pgvector

Database service:

- PostgreSQL 16 in the legacy Docker setup;
- pgvector extension enabled;
- embeddings are stored as 1536-dimensional vectors.

The Java migration must verify driver/library compatibility and actual database extension state.

---

## 5. Docker

Legacy deployment uses a multi-stage .NET Dockerfile.

The existing Docker Compose environment includes at least:

- PostgreSQL;
- backend API;
- AI service in the broader development setup/documentation.

The Java Dockerfile should be introduced only after the Java application runs reliably outside Docker.

During transition, avoid changing all container orchestration concerns at the same time as core API migration.

---

## 6. Configuration Names

The legacy backend reads configuration from environment/config providers, including concepts such as:

- database connection;
- AI service URL;
- JWT issuer/audience/secret;
- email settings;
- Cloudinary settings;
- optional embedding bootstrap/backfill flags.

The Java project may normalize property names, but container/deployment compatibility must be considered before renaming externally supplied environment variables.
