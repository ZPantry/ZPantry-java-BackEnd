package com.zpantry.migration;

import com.zpantry.authentication.service.AuthenticationService;
import com.zpantry.authentication.service.EmailVerificationPort;
import com.zpantry.foundation.IsolatedPostgres;
import com.zpantry.ingredient.service.IngredientService;
import com.zpantry.ingredient.persistence.IngredientRepository;
import com.zpantry.integration.ai.AiClient;
import com.zpantry.media.service.MediaStoragePort;
import com.zpantry.pantry.service.PantryService;
import com.zpantry.pantryimport.api.PantryImportDtos.SourceType;
import com.zpantry.pantryimport.service.PantryImportService;
import com.zpantry.pantryimport.service.PantryIngredientPipeline;
import com.zpantry.pantry.service.PantryTextExtractionService;
import com.zpantry.recipe.service.RecipeService;
import com.zpantry.user.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.zpantry.authentication.api.AuthenticationDtos.*;
import static com.zpantry.ingredient.api.IngredientDtos.*;
import static com.zpantry.pantry.api.PantryDtos.UpsertPantryItemRequest;
import static com.zpantry.pantry.api.PantryDtos.UpdatePantryItemRequest;
import static com.zpantry.recipe.api.RecipeDtos.RecipeIngredientRequest;
import static com.zpantry.recipe.api.RecipeDtos.RecipeRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true", "spring.flyway.baseline-on-migrate=false",
        "zpantry.security.jwt.enabled=false", "zpantry.security.jwt.secret=TEST_ONLY_batch_migration_key_longer_than_32_bytes"})
@ContextConfiguration(classes = {com.zpantry.ZPantryBackendApplication.class, IsolatedPostgres.class, RemainingFeaturesIT.Fakes.class})
class RemainingFeaturesIT {
    @org.springframework.beans.factory.annotation.Autowired
    AuthenticationService auth;
    @org.springframework.beans.factory.annotation.Autowired
    CapturingEmail email;
    @org.springframework.beans.factory.annotation.Autowired
    UserRepository users;
    @org.springframework.beans.factory.annotation.Autowired
    IngredientService ingredients;
    @org.springframework.beans.factory.annotation.Autowired
    IngredientRepository ingredientRepository;
    @org.springframework.beans.factory.annotation.Autowired
    RecipeService recipes;
    @org.springframework.beans.factory.annotation.Autowired
    PantryService pantry;
    @org.springframework.beans.factory.annotation.Autowired
    PantryImportService pantryImport;
    @org.springframework.beans.factory.annotation.Autowired
    PantryIngredientPipeline pantryPipeline;
    @org.springframework.beans.factory.annotation.Autowired
    PantryTextExtractionService pantryTextExtraction;

    @Test
    void authenticationRegisterVerifyLoginRefreshAndRotation() {
        String address = "batch-auth@test.dev";
        auth.register(new RegisterRequest("Batch", address, "test-password"));
        assertThat(email.otp).matches("\\d{6}");
        assertThat(auth.verify(new VerifyOtpRequest(email.otp, address))).isTrue();
        var login = auth.login(new LoginRequest(address, "test-password"));
        assertThat(login.accessToken()).hasSizeGreaterThan(100);
        assertThat(login.refreshToken()).hasSize(86);
        var refreshed = auth.refresh(new RefreshTokenRequest(login.refreshToken()));
        assertThat(refreshed.refreshToken()).isNotEqualTo(login.refreshToken());
    }

    @Test
    void passwordResetUsesOtpChangesPasswordAndInvalidatesRefreshToken() {
        String address = "reset-auth-" + UUID.randomUUID() + "@test.dev";
        auth.register(new RegisterRequest("Reset", address, "old-password"));
        assertThat(auth.verify(new VerifyOtpRequest(email.otp, address))).isTrue();
        var login = auth.login(new LoginRequest(address, "old-password"));

        auth.forgotPassword(new ForgotPasswordRequest(address));
        String otp = email.otp;
        String wrongOtp = "000000".equals(otp) ? "000001" : "000000";
        assertThat(auth.resetPassword(new ResetPasswordRequest(address, otp, "new-password", "different-password"))).isFalse();
        assertThat(auth.resetPassword(new ResetPasswordRequest(address, wrongOtp, "new-password", "new-password"))).isFalse();
        assertThat(auth.resetPassword(new ResetPasswordRequest(address, otp, "new-password", "new-password"))).isTrue();
        assertThatThrownBy(() -> auth.refresh(new RefreshTokenRequest(login.refreshToken())))
                .isInstanceOf(com.zpantry.authentication.service.AuthenticationFailure.class);
        assertThat(auth.login(new LoginRequest(address, "new-password")).email()).isEqualTo(address);
    }

    @Test
    void ingredientRecipeAndPantryPersistWithVectorMappings() {
        String ingredientName = "Rice " + UUID.randomUUID();
        var ingredient = ingredients.create(new CreateIngredientRequest(ingredientName, "grain", "g", BigDecimal.ONE, null, null, null, null, null, null));
        assertThat(ingredient.success()).isTrue();
        assertThat(ingredient.data().normalizedName()).isEqualTo(ingredientName.toLowerCase());
        var recipe = recipes.create(new RecipeRequest("Rice bowl " + UUID.randomUUID(), null, 10, "easy", 1, "Cook", null, "manual", null, null, List.of(new RecipeIngredientRequest(ingredient.data().id(), ingredientName, BigDecimal.ONE, "g", true, null))));
        assertThat(recipe.success()).isTrue();
        UUID user = UUID.randomUUID();
        var item = pantry.upsert(user, new UpsertPantryItemRequest(ingredient.data().id(), BigDecimal.TEN, "g", null, null, null));
        assertThat(item.success()).isTrue();
        assertThat(pantry.list(user, 1, 10).data()).hasSize(1);
        assertThat(recipes.get(recipe.data().id()).data().ingredients()).hasSize(1);
    }

    @Test
    void ingredientBatchCreatesCanonicalUnitsAndDefaultQuantities() {
        String suffix = UUID.randomUUID().toString();
        var result = ingredients.createBatch(List.of(
                new CreateIngredientRequest("Batch Rice " + suffix, "carbohydrate", "g", null, null, null, null, null, null, null),
                new CreateIngredientRequest("Batch Egg " + suffix, "protein", "quả", null, null, null, null, null, null, null)));

        assertThat(result.success()).isTrue();
        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).extracting(item -> item.unit()).containsExactly("g", "quả");
        assertThat(result.data()).extracting(item -> item.defaultQuantity()).satisfiesExactly(
                quantity -> assertThat(quantity).isEqualByComparingTo("100"),
                quantity -> assertThat(quantity).isEqualByComparingTo("1"));
    }

    @Test
    void pantryBatchSavesAllDistinctItemsForOneUser() {
        var first = ingredients.create(new CreateIngredientRequest("Pantry batch rice " + UUID.randomUUID(), "grain", "g", null, null, null, null, null, null, null));
        var second = ingredients.create(new CreateIngredientRequest("Pantry batch egg " + UUID.randomUUID(), "protein", "quả", null, null, null, null, null, null, null));
        UUID userId = UUID.randomUUID();

        var result = pantry.upsertBatch(userId, List.of(
                new UpsertPantryItemRequest(first.data().id(), BigDecimal.valueOf(500), "g", null, "kitchen", null),
                new UpsertPantryItemRequest(second.data().id(), BigDecimal.valueOf(6), "quả", null, "kitchen", null)));

        assertThat(result.success()).isTrue();
        assertThat(result.data()).hasSize(2);
        assertThat(pantry.list(userId, 1, 10).totalItems()).isEqualTo(2);
    }

    @Test
    void ingredientCreationPersistsBeforeEmbeddingAndPantryRejectsInvalidUpdates() {
        var ingredient = ingredients.create(new CreateIngredientRequest("Validated rice", "grain", "g", null, null, null, null, null, null, null));
        assertThat(ingredient.success()).isTrue();
        assertThat(ingredient.data().id()).isNotNull();
        UUID user = UUID.randomUUID();
        Instant expiry = Instant.now().plusSeconds(86_400);
        var item = pantry.upsert(user, new UpsertPantryItemRequest(ingredient.data().id(), BigDecimal.TEN, "g", expiry, null, null));
        assertThat(item.success()).isTrue();
        var invalid = pantry.update(user, item.data().id(), new UpdatePantryItemRequest(null, BigDecimal.valueOf(-1), null, null, null, null), false);
        assertThat(invalid.success()).isFalse();
        assertThat(pantry.list(user, 1, 10).data().getFirst().quantity()).isEqualByComparingTo(BigDecimal.TEN);
        var clearsExpiry = pantry.update(user, item.data().id(), new UpdatePantryItemRequest(null, null, null, null, null, null), true);
        assertThat(clearsExpiry.success()).isTrue();
        assertThat(clearsExpiry.data().expiredAt()).isNull();
    }

    @Test
    void imageImportResolvesAReceiptProductLabelToTheCanonicalCatalogIngredient() {
        var ingredient = ingredientRepository.findByNormalizedNameAndDeletedFalse("gạo").orElseThrow();
        var image = new MockMultipartFile("image", "rice.png", "image/png", new byte[] {1, 2, 3});

        var preview = pantryImport.analyze(SourceType.FOOD_IMAGE, image);

        assertThat(preview.items()).singleElement().satisfies(item -> {
            assertThat(item.ingredientId()).isEqualTo(ingredient.getId());
            assertThat(item.rawName()).isEqualTo("Gạo ST25 2kg");
            assertThat(item.canonicalIngredientName()).isEqualTo("Gạo");
            assertThat(item.ingredient()).isNotNull();
            assertThat(item.ingredient().id()).isEqualTo(item.ingredientId());
            assertThat(item.unit()).isEqualTo("g");
            assertThat(item.quantity()).isPositive();
        });
    }

    @Test
    void localTextExtractionReusesAliasMatchingAndTheSamePantryPipeline() {
        var rows = pantryTextExtraction.extract("I have 500g white rice and 2 chicken egg").stream()
                .map(pantryPipeline::resolve)
                .flatMap(Optional::stream)
                .toList();

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(item -> item.canonicalIngredientName()).containsExactly("Rice", "Egg");
        assertThat(rows).extracting(item -> item.ingredientId()).doesNotContainNull();
        assertThat(rows.getFirst().quantity()).isEqualByComparingTo("500");
        assertThat(rows.get(1).quantity()).isEqualByComparingTo("2");
    }

    @Test
    void ingredientSearchFiltersBeforePagingAndReportsFilteredTotal() {
        ingredients.create(new CreateIngredientRequest("Parity Apple", "parity-fruit", null, null, null, null, null, null, null, null));
        ingredients.create(new CreateIngredientRequest("Parity Banana", "parity-fruit", null, null, null, null, null, null, null, null));
        ingredients.create(new CreateIngredientRequest("Parity Carrot", "vegetable", null, null, null, null, null, null, null, null));
        var result = ingredients.list(1, 1, "parity-fruit");
        assertThat(result.data()).hasSize(1);
        assertThat(result.totalItems()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void ingredientV2UpdatePersistsUploadedImageAndRejectsDuplicateRename() {
        var first = ingredients.create(new CreateIngredientRequest("Parity First", null, null, null, null, null, null, null, null, null));
        var second = ingredients.create(new CreateIngredientRequest("Parity Second", null, null, null, null, null, null, null, null, null));
        var file = new MockMultipartFile("imageFile", "photo.png", "image/png", new byte[]{1, 2, 3});
        var updated = ingredients.updateForm(first.data().id(), new IngredientFormRequest(null, null, null, null, null, null, null, null, null, null, file));
        assertThat(updated.success()).isTrue();
        assertThat(updated.data().imageUrl()).isEqualTo("https://test");
        var duplicate = ingredients.update(first.data().id(), new UpdateIngredientRequest(second.data().name(), null, null, null, null, null, null, null, null, null));
        assertThat(duplicate.success()).isFalse();
        assertThat(duplicate.message()).isEqualTo("Ingredient already exists.");
    }

    @Configuration
    static class Fakes {
        @Bean
        @Primary
        AiClient ai() {
            return new AiClient() {
                public Optional<float[]> embedIngredient(UUID i, String n, String nn, String c) {
                    return Optional.of(new float[1536]);
                }

                public Optional<float[]> embedRecipe(UUID i, String n, String d, List<String> x, String t) {
                    return Optional.of(new float[1536]);
                }

                public Map<String, Object> post(String p, Object r) {
                    return Map.of("success", true, "data", Map.of("items", List.of()));
                }

                @Override
                public Map<String, Object> postImage(String path, byte[] image, String filename, String contentType) {
                    return Map.of("success", true, "data", Map.of("items", List.of(Map.of(
                            "name", "Gạo ST25 2kg", "quantity", 0, "unit", "piece",
                            "food", true))));
                }
            };
        }

        @Bean
        @Primary
        MediaStoragePort media() {
            return new MediaStoragePort() {
                public UploadResult upload(org.springframework.web.multipart.MultipartFile f, String folder) {
                    return new UploadResult("test/public", "http://test", "https://test", "image", "png", 1, 1);
                }

                public void delete(String id) {
                }
            };
        }

        @Bean
        @Primary
        CapturingEmail email() {
            return new CapturingEmail();
        }
    }

    static class CapturingEmail implements EmailVerificationPort {
        String otp;

        public void sendVerification(String e, String n, String o) {
            otp = o;
        }
    }
}
