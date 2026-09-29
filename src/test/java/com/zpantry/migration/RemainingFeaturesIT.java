package com.zpantry.migration;

import com.zpantry.authentication.service.AuthenticationService;
import com.zpantry.authentication.service.EmailVerificationPort;
import com.zpantry.foundation.IsolatedPostgres;
import com.zpantry.ingredient.service.IngredientService;
import com.zpantry.integration.ai.AiClient;
import com.zpantry.media.service.MediaStoragePort;
import com.zpantry.pantry.service.PantryService;
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
    RecipeService recipes;
    @org.springframework.beans.factory.annotation.Autowired
    PantryService pantry;

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
    void ingredientSearchFiltersBeforePagingAndReportsFilteredTotal() {
        ingredients.create(new CreateIngredientRequest("Parity Apple", "fruit", null, null, null, null, null, null, null, null));
        ingredients.create(new CreateIngredientRequest("Parity Banana", "fruit", null, null, null, null, null, null, null, null));
        ingredients.create(new CreateIngredientRequest("Parity Carrot", "vegetable", null, null, null, null, null, null, null, null));
        var result = ingredients.list(1, 1, "fruit");
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
                    return Map.of();
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
