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

## Java backend container

`Dockerfile` is a multi-stage Java 21 build. It resolves Maven dependencies, packages the Spring
Boot executable jar, then runs it in a Java 21 JRE image as the non-root `zpantry` user. It contains
no database, JWT, Cloudinary, or Gemini secrets. The workspace Compose stack builds it through
`D:/zpantry/compose.yaml` and provides development-only database and AI-service endpoints as
environment variables.

From `D:/zpantry`, start the development stack with:

```powershell
docker compose up --build
```

For a non-development deployment, provide an explicit Spring profile and all required datasource,
JWT, AI-service, media, and CORS environment variables through the deployment platform; do not copy
a private `.env` file into the image.

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
## Gemini image analysis

The independent AI service exposes `POST /ai/analyze-receipt` and
`POST /ai/recognize-food-image`. It reads `GEMINI_API_KEY` and optional `GEMINI_MODEL` from its
local `.env`; these values must never be present in Java configuration, frontend code, logs, or Git.
The Java backend calls the AI service through `AI_SERVICE_URL` using multipart image uploads.

### Pantry AI catalog boundary

`POST /api/me/pantry/parse`, `/api/me/pantry-import/receipt/analyze`, and
`/api/me/pantry-import/food-image/analyze` pass untrusted extraction hints through a
shared backend catalog boundary resolves every returned row to exactly one active `ingredients`
record before it is exposed: `ingredientId`, `canonicalIngredientName`, `ingredient` and `unit`
therefore come from PostgreSQL, never from Gemini. Suggested quantities are used only
when positive; otherwise the persisted `defaultQuantity` is returned. AI detections that cannot be
resolved unambiguously are omitted and reported only as a preview warning for image flows.

Text parsing is local and deterministic: it segments explicit comma/newline/conjunction-separated
items and then matches canonical food names or maintained aliases. It makes no AI-service, Ollama,
or provider-key call. A text item that does not map uniquely to the active catalog is not returned.

For Gemini image analysis, the AI service additionally canonicalizes `items`, `ingredients`, or a
root array, and accepts item names delivered as `rawName`, `name`, `item`, `productName`, or
`product`. The backend accepts the same name fields as a defence-in-depth boundary. A unique
canonical catalog name embedded in a receipt product label is valid (for example `Sữa tươi` in
`Sữa tươi TH 1L`); ambiguous labels remain omitted rather than being guessed.

## Recommendation V2 and future analysis boundary

`POST /api/recommendations/v2/meals` is now provider-independent: Java ranks active catalog recipes
using authenticated Pantry data, declared recipe allergens, required ingredient coverage and an
expiring-soon bonus. It makes no AI-service call, so provider outage, quota exhaustion and malformed
model output cannot prevent a user from receiving their ranked catalog results. The legacy
`/ai/recommend-meals` route remains available only for the legacy recommendation path.

A future conversational-analysis operation may call the AI service only after the backend has fixed
the ranked recipe set. Its payload must be privacy-minimized and contain server-derived recipe IDs,
their ingredient facts and the permitted profile context; the response must be validated against those
IDs before it is shown. It is not implemented by the deterministic ranking endpoint.

## Image-analysis usage limit

Before forwarding a food image, receipt image or unified image to the AI service, Java atomically
consumes one authenticated user's monthly allowance. The default is three requests per calendar month
in the configured Vietnam time zone. The limit is configured with
`ZPANTRY_AI_IMAGE_ANALYSIS_MONTHLY_LIMIT`; subscription work can assign a different allowance through
the future entitlement boundary without changing the AI client. A provider failure still consumes a
request because the provider call was attempted. When exhausted, Java returns HTTP 429 without making
the provider call.
