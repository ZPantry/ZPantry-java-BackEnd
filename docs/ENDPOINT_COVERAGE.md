# Legacy-to-Java Endpoint Coverage

Pinned legacy commit: `a010fdc5894176596bb195e4fef66db2c09496f1`. Updated 2026-09-19.

| Legacy method | Java method | Status |
|---|---|---|
| POST `/api/Auth/register` | `AuthenticationController.register` | INTENTIONAL_DEVIATION: rejects blank email/password; secure OTP RNG |
| POST `/api/Auth/verify-otp` | `AuthenticationController.verify` | PARITY |
| POST `/api/Auth/login` | `AuthenticationController.login` | PARITY |
| POST `/api/Auth/refresh-token` | `AuthenticationController.refresh` | PARITY |
| POST `/api/Auth/logout` | `AuthenticationController.logout` | INTENTIONAL_DEVIATION: bounded Java-local revocation |
| GET `/api/users` | `UserController.list` | PARITY |
| GET `/api/users/{id}` | `UserController.get` | PARITY |
| PUT `/api/users/{id}` | `UserController.update` | INTENTIONAL_DEVIATION: ADR-012 |
| DELETE `/api/users/{id}` | `UserController.delete` | PARITY |
| GET `/api/ingredients` | `IngredientController.list` | PARTIALLY_VERIFIED: PostgreSQL search-before-paging, totals and vector-backed persistence covered; HTTP fixture parity missing |
| POST `/api/ingredients` | `IngredientController.create` | IMPLEMENTED; endpoint parity not captured |
| POST `/api/v2/ingredients` | `IngredientController.createV2` | IMPLEMENTED; external media unverified |
| PUT `/api/ingredients/{id}` | `IngredientController.update` | PARTIALLY_VERIFIED: duplicate-name behavior covered; HTTP fixture parity missing |
| PUT `/api/v2/ingredients/{id}` | `IngredientController.updateV2` | PARTIALLY_VERIFIED: deterministic fake upload persistence covered; HTTP fixture and real media remain unverified |
| DELETE `/api/ingredients/{id}` | `IngredientController.delete` | IMPLEMENTED; endpoint parity not captured |
| GET `/api/recipes` | `RecipeController.list` | IMPLEMENTED; endpoint parity not captured |
| POST `/api/recipes` | `RecipeController.create` | IMPLEMENTED; endpoint parity not captured |
| POST `/api/v2/recipes` | `RecipeController.createV2` | IMPLEMENTED; external media unverified |
| GET `/api/recipes/{id}` | `RecipeController.get` | IMPLEMENTED; endpoint parity not captured |
| PUT `/api/recipes/{id}` | `RecipeController.update` | IMPLEMENTED; endpoint parity not captured |
| PUT `/api/v2/recipes/{id}` | `RecipeController.updateV2` | IMPLEMENTED; external media unverified |
| DELETE `/api/recipes/{id}` | `RecipeController.delete` | IMPLEMENTED; endpoint parity not captured |
| POST `/api/media/upload` | `MediaController.upload` | IMPLEMENTED; Cloudinary unverified |
| DELETE `/api/media` | `MediaController.delete` | IMPLEMENTED; Cloudinary unverified |
| GET `/api/me/pantry` | `PantryController.get` | IMPLEMENTED; endpoint parity not captured |
| GET `/api/me/pantry/items` | `PantryController.items` | IMPLEMENTED; endpoint parity not captured |
| POST `/api/me/pantry/items` | `PantryController.upsert` | IMPLEMENTED; endpoint parity not captured |
| PUT `/api/me/pantry/items/{itemId}` | `PantryController.update` | IMPLEMENTED; endpoint parity not captured |
| DELETE `/api/me/pantry/items/{itemId}` | `PantryController.delete` | IMPLEMENTED; endpoint parity not captured |
| POST `/api/recommendations/meals` | `RecommendationController.meals` | IMPLEMENTED; upstream contract unverified |
| POST `/api/recommendations/missing-ingredients` | `RecommendationController.missing` | IMPLEMENTED; upstream contract unverified |
| GET `/api/recommendations/meals/{mealId}/missing-ingredients` | `RecommendationController.check` | IMPLEMENTED; upstream contract unverified |
| GET `/api/recommendations/{id}` | `RecommendationController.get` | IMPLEMENTED; endpoint parity not captured |
| POST `/api/recommendations/{id}/feedback` | `RecommendationController.feedback` | IMPLEMENTED; endpoint parity not captured |
| GET `/api/me/today-menu` | `TodayMenuController.list` | IMPLEMENTED; endpoint parity not captured |
| GET `/api/me/today-menu/items/{id}` | `TodayMenuController.get` | IMPLEMENTED; detail expansion not parity-verified |
| POST `/api/me/today-menu/items` | `TodayMenuController.create` | IMPLEMENTED; endpoint parity not captured |
| DELETE `/api/me/today-menu/items/{id}` | `TodayMenuController.delete` | IMPLEMENTED; endpoint parity not captured |
| POST `/api/me/today-menu/items/{id}/complete` | `TodayMenuController.complete` | IMPLEMENTED; transaction/media compensation needs parity tests |
| GET `/api/me/cooking-logs` | `TodayMenuController.logs` | IMPLEMENTED; endpoint parity not captured |

No legacy public controller method is absent from Java. “Implemented” does not mean parity-certified.

The system-wide verification pass has not finalized this matrix. Current counts are 6
`PARITY`/parity-verified endpoints, 3 documented intentional deviations, 3 partially verified
Ingredient endpoints and 28 implemented endpoints without sufficient parity evidence. R-033 and
R-034 are confirmed behavioral defects, so affected endpoints cannot be relabeled as verified.
