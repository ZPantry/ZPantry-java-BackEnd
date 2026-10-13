package com.zpantry.todaymenu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zpantry.ingredient.persistence.IngredientRepository;
import com.zpantry.media.service.MediaStoragePort;
import com.zpantry.pantry.persistence.PantryItemRepository;
import com.zpantry.recipe.domain.RecipeEntity;
import com.zpantry.recipe.persistence.RecipeIngredientRepository;
import com.zpantry.recipe.persistence.RecipeRepository;
import com.zpantry.todaymenu.api.TodayMenuDtos.CompleteTodayMenuItemRequest;
import com.zpantry.todaymenu.domain.CookingLogEntity;
import com.zpantry.todaymenu.domain.TodayMenuItemEntity;
import com.zpantry.todaymenu.persistence.CookingLogRepository;
import com.zpantry.todaymenu.persistence.PantryUsageLogRepository;
import com.zpantry.todaymenu.persistence.ShoppingListItemRepository;
import com.zpantry.todaymenu.persistence.TodayMenuItemRepository;
import com.zpantry.user.service.AllergenSafetyService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class TodayMenuServiceTest {
    private final TodayMenuItemRepository menus = org.mockito.Mockito.mock(TodayMenuItemRepository.class);
    private final CookingLogRepository logs = org.mockito.Mockito.mock(CookingLogRepository.class);
    private final PantryUsageLogRepository usage = org.mockito.Mockito.mock(PantryUsageLogRepository.class);
    private final MediaStoragePort media = org.mockito.Mockito.mock(MediaStoragePort.class);
    private final RecipeRepository recipes = org.mockito.Mockito.mock(RecipeRepository.class);
    private final RecipeIngredientRepository recipeIngredients = org.mockito.Mockito.mock(RecipeIngredientRepository.class);
    private final PantryItemRepository pantry = org.mockito.Mockito.mock(PantryItemRepository.class);
    private final IngredientRepository ingredients = org.mockito.Mockito.mock(IngredientRepository.class);
    private final ShoppingListItemRepository shoppingList = org.mockito.Mockito.mock(ShoppingListItemRepository.class);
    private final AllergenSafetyService allergenSafety = org.mockito.Mockito.mock(AllergenSafetyService.class);
    private final TodayMenuService service = new TodayMenuService(menus, logs, usage, media, recipes,
            recipeIngredients, pantry, ingredients, shoppingList, allergenSafety);

    @Test
    void completesAndSavesTheCookingLogWithoutAnImage() {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();
        TodayMenuItemEntity item = new TodayMenuItemEntity(userId);
        item.recipeId = recipeId;
        item.mealName = "Món thử nghiệm";
        item.servingSize = 1;
        RecipeEntity recipe = new RecipeEntity("Món thử nghiệm");

        when(menus.findByIdAndUserIdAndDeletedFalse(itemId, userId)).thenReturn(Optional.of(item));
        when(recipes.findByIdAndDeletedFalse(recipeId)).thenReturn(Optional.of(recipe));
        when(logs.save(any(CookingLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recipeIngredients.findAllByRecipeIdAndDeletedFalse(recipeId)).thenReturn(List.of());
        when(pantry.findAllByUserIdAndDeletedFalseOrderByExpiredAtAscCreatedAtAsc(userId)).thenReturn(List.of());

        var response = service.complete(userId, itemId,
                new CompleteTodayMenuItemRequest(null, Instant.parse("2026-10-13T11:00:00Z"), 5, "Ngon"));

        assertThat(response.success()).isTrue();
        assertThat(item.status).isEqualTo("Cooked");
        assertThat(response.data().cookingLog().imageUrl()).isNull();
        verify(media, never()).upload(any(), any());
    }

    @Test
    void completesWhenTheOptionalImageUploadFails() {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();
        TodayMenuItemEntity item = new TodayMenuItemEntity(userId);
        item.recipeId = recipeId;
        item.mealName = "Món thử nghiệm";
        item.servingSize = 1;
        RecipeEntity recipe = new RecipeEntity("Món thử nghiệm");
        var image = new MockMultipartFile("imageFile", "meal.png", "image/png", new byte[]{1});

        when(menus.findByIdAndUserIdAndDeletedFalse(itemId, userId)).thenReturn(Optional.of(item));
        when(recipes.findByIdAndDeletedFalse(recipeId)).thenReturn(Optional.of(recipe));
        when(logs.save(any(CookingLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recipeIngredients.findAllByRecipeIdAndDeletedFalse(recipeId)).thenReturn(List.of());
        when(pantry.findAllByUserIdAndDeletedFalseOrderByExpiredAtAscCreatedAtAsc(userId)).thenReturn(List.of());
        when(media.upload(image, "cooking")).thenThrow(new IllegalStateException("Media unavailable"));

        var response = service.complete(userId, itemId,
                new CompleteTodayMenuItemRequest(image, Instant.parse("2026-10-13T11:00:00Z"), 5, "Ngon"));

        assertThat(response.success()).isTrue();
        assertThat(item.status).isEqualTo("Cooked");
        assertThat(response.data().warnings()).contains("Completion image could not be uploaded.");
    }
}
