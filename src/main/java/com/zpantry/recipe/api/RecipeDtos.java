package com.zpantry.recipe.api;

import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.zpantry.user.domain.FoodAllergen;

public final class RecipeDtos {
    private RecipeDtos() {
    }

    public record RecipeIngredientResponse(UUID ingredientId, String ingredientName, BigDecimal quantity, String unit,
                                           boolean isRequired, String note) {
    }

    public record RecipeResponse(UUID id, String name, String description, Integer cookingTimeMinutes,
                                 String difficulty, Integer servingSize, String instructionText, String imageUrl,
                                 String sourceType, String gradientFrom, String gradientTo,
                                 List<RecipeIngredientResponse> ingredients, Set<FoodAllergen> allergens) {
    }

    public record RecipeIngredientRequest(UUID ingredientId, String ingredientName, BigDecimal quantity, String unit,
                                          boolean isRequired, String note) {
    }

    public record RecipeRequest(String name, String description, Integer cookingTimeMinutes, String difficulty,
                                Integer servingSize, String instructionText, String imageUrl, String sourceType,
                                String gradientFrom, String gradientTo, List<RecipeIngredientRequest> ingredients, Set<FoodAllergen> allergens) {
        public RecipeRequest(String name, String description, Integer cookingTimeMinutes, String difficulty,
                Integer servingSize, String instructionText, String imageUrl, String sourceType,
                String gradientFrom, String gradientTo, List<RecipeIngredientRequest> ingredients) {
            this(name, description, cookingTimeMinutes, difficulty, servingSize, instructionText, imageUrl,
                    sourceType, gradientFrom, gradientTo, ingredients, Set.of());
        }
    }

    public record RecipeFormRequest(String name, String description, Integer cookingTimeMinutes, String difficulty,
                                    Integer servingSize, String instructionText, String imageUrl, String sourceType,
                                    String gradientFrom, String gradientTo, String ingredientsJson,
                                    MultipartFile imageFile, Set<FoodAllergen> allergens) {
    }
}
