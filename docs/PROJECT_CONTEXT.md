# ZPantry — Project Context

## 1. Product Overview

ZPantry is a pantry and meal-management backend.

The system combines ordinary application data with AI-assisted meal recommendation and embedding-based data support.

Core capabilities include:

- account registration and login;
- email/OTP verification;
- user management;
- ingredient data;
- recipe data;
- personal pantry inventory;
- meal recommendation;
- missing-ingredient analysis;
- daily meal planning;
- cooking completion/history;
- pantry usage tracking;
- media/image management;
- integration with an external AI service;
- vector embeddings stored in PostgreSQL/pgvector.

The original backend is implemented in ASP.NET Core/.NET 10.

The Java migration exists to replace the backend implementation while retaining the behavior expected by existing clients and data.

---

## 2. Migration Objective

The migration is not a greenfield rewrite.

The Java system should initially preserve:

- API behavior;
- user/account continuity where practical;
- existing PostgreSQL data;
- JWT semantics required by clients;
- business flows;
- AI-service integration behavior;
- Cloudinary/media behavior;
- soft-delete rules;
- recommendation/history persistence.

Architectural improvements are allowed only when they do not silently break compatibility, or when an intentional incompatibility is explicitly approved and documented.

---

## 3. Main Domain Areas

### User and Authentication

Responsible for:

- registration;
- OTP verification;
- login;
- access-token creation;
- refresh-token rotation;
- logout/revocation behavior;
- profile updates;
- administration of users.

### Ingredient

Represents canonical food ingredients and their metadata.

Important concepts include:

- canonical name;
- normalized name;
- category;
- unit;
- nutritional values;
- aliases;
- image;
- embedding.

### Recipe

Represents a meal/recipe and its ingredient requirements.

Important concepts include:

- recipe metadata;
- preparation instructions;
- cooking time;
- difficulty;
- serving size;
- source type;
- recipe ingredients;
- image;
- embedding.

### Pantry

Represents the ingredients owned by a user.

Important concepts include:

- ingredient;
- quantity;
- unit;
- expiration;
- storage location;
- notes.

### Recommendation

Coordinates user input/pantry data with the external AI service and persists recommendation results and feedback.

### Today Menu

Represents meals a user plans to cook on a specific date.

Completing a menu item can lead to:

- image upload;
- cooking log creation;
- pantry usage records;
- pantry quantity updates;
- warnings when pantry consumption cannot be applied cleanly.

### Media

Uploads and deletes media using Cloudinary and associates media with ingredients/recipes where applicable.

### AI Integration

The backend is not itself the main AI inference service.

It calls a separate AI service over HTTP for recommendation, missing-ingredient analysis, meal checks, completion checks, and embeddings.

---

## 4. Conceptual Domain Flow

```text
User
 │
 ├──────────────► Pantry
 │                  │
 │                  │ pantry/selected ingredients
 │                  ▼
 ├──────────────► Recommendation ─────────► AI Service
 │                  │
 │                  ▼
 │           MealRecommendation
 │                  │
 │                  ▼
 └──────────────► Today Menu
                    │
                    │ complete meal
                    ▼
                Cooking Log
                    │
                    ▼
              Pantry Usage Log
                    │
                    ▼
              Pantry adjustment
```

Ingredient and Recipe form the shared food catalog:

```text
Ingredient ──► RecipeIngredient ◄── Recipe

Ingredient ──► UserPantryItem
Recipe     ──► Recommendation result
Recipe     ──► TodayMenuItem
```

---

## 5. Important Business Invariants

The Java migration should verify and preserve these behaviors where supported by the legacy implementation:

- soft-deleted records are normally excluded from ordinary reads;
- user-owned resources must be scoped by the authenticated user;
- admin-only user-management operations remain admin-only;
- refresh tokens are stored as hashes rather than raw tokens;
- recommendation records belong to a user;
- pantry changes caused by cooking must remain attributable through logs;
- embeddings use 1536 dimensions in the legacy schema;
- AI failure must not be silently treated as a successful recommendation.

---

## 6. What This Document Does Not Define

This file intentionally does not duplicate exact endpoint-by-endpoint API definitions.

Use the existing repository file:

`API_MIGRATION.md`

for API migration details and status.

Generic Java/Spring coding conventions belong to `.agents/skills/`, not here.
