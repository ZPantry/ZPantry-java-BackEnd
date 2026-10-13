package com.zpantry.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.zpantry.ingredient.domain.IngredientEntity;
import com.zpantry.ingredient.persistence.IngredientRepository;
import com.zpantry.recipe.domain.RecipeIngredientEntity;
import com.zpantry.recipe.persistence.RecipeIngredientRepository;
import com.zpantry.user.domain.UserProfileEntity;
import com.zpantry.user.persistence.UserProfileRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AllergenSafetyServiceTest {
    @Mock private UserProfileRepository profiles;
    @Mock private IngredientRepository ingredients;
    @Mock private RecipeIngredientRepository recipeIngredients;
    @Mock private UserProfileEntity profile;

    @Test
    void blocksIngredientThatMatchesTheUsersAllergy() {
        UUID userId = UUID.randomUUID();
        UUID ingredientId = UUID.randomUUID();
        IngredientEntity milk = new IngredientEntity("Sữa tươi");
        milk.allergens = "MILK";
        when(profiles.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));
        when(profile.getAllergies()).thenReturn("MILK");
        when(ingredients.findByIdAndDeletedFalse(ingredientId)).thenReturn(Optional.of(milk));

        String conflict = service().ingredientConflict(userId, List.of(ingredientId));

        assertThat(conflict).contains("Sữa tươi").contains("sữa");
    }

    @Test
    void blocksRecipeWhenARequiredIngredientMatchesTheUsersAllergy() {
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();
        UUID ingredientId = UUID.randomUUID();
        IngredientEntity egg = new IngredientEntity("Trứng gà");
        egg.allergens = "EGG";
        RecipeIngredientEntity link = new RecipeIngredientEntity(recipeId, ingredientId, BigDecimal.ONE, "quả", true, null);
        when(profiles.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));
        when(profile.getAllergies()).thenReturn("EGG");
        when(recipeIngredients.findAllByRecipeIdAndDeletedFalse(recipeId)).thenReturn(List.of(link));
        when(ingredients.findByIdAndDeletedFalse(ingredientId)).thenReturn(Optional.of(egg));

        String conflict = service().recipeConflict(userId, recipeId);

        assertThat(conflict).contains("thực đơn").contains("Trứng gà").contains("trứng");
    }

    private AllergenSafetyService service() {
        return new AllergenSafetyService(profiles, ingredients, recipeIngredients);
    }
}
