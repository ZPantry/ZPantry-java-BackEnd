package com.zpantry.ingredient.api;

import com.zpantry.user.domain.FoodAllergen;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public final class IngredientDtos {
    private IngredientDtos() {
    }

    public record IngredientResponse(UUID id, String name, String normalizedName, String category, String unit,
                                     BigDecimal caloriesPerUnit, BigDecimal proteinPerUnit, BigDecimal fatPerUnit,
                                     BigDecimal carbPerUnit, BigDecimal defaultQuantity, String imageUrl, String gradientFrom, String gradientTo,
                                     Set<FoodAllergen> allergens) {
    }

    public record CreateIngredientRequest(@NotBlank @Size(max = 200) String name, String category,
                                          @NotBlank @Size(max = 50) String unit, BigDecimal caloriesPerUnit,
                                          BigDecimal protenPerUnit, BigDecimal fatPerUnit, BigDecimal carbPerUnit,
                                          BigDecimal defaultQuantity, String imageUrl, String gradientFrom, String gradientTo,
                                          Set<FoodAllergen> allergens) {
        public CreateIngredientRequest(String name, String category, String unit, BigDecimal calories,
                BigDecimal protein, BigDecimal fat, BigDecimal carb, String image, String from, String to) {
            this(name, category, unit, calories, protein, fat, carb, null, image, from, to, Set.of());
        }

        public CreateIngredientRequest(String name, String category, String unit, BigDecimal calories,
                BigDecimal protein, BigDecimal fat, BigDecimal carb, String image, String from, String to,
                Set<FoodAllergen> allergens) {
            this(name, category, unit, calories, protein, fat, carb, null, image, from, to, allergens);
        }
    }

    public record CreateIngredientBatchRequest(@NotEmpty java.util.List<@jakarta.validation.Valid CreateIngredientRequest> ingredients) {}

    public record UpdateIngredientRequest(String name, String category, String unit, BigDecimal caloriesPerUnit,
                                          BigDecimal proteinPerUnit, BigDecimal fatPerUnit, BigDecimal carbPerUnit,
                                          String imageUrl, String gradientFrom, String gradientTo,
                                          Set<FoodAllergen> allergens) {
        public UpdateIngredientRequest(String name, String category, String unit, BigDecimal calories,
                BigDecimal protein, BigDecimal fat, BigDecimal carb, String image, String from, String to) {
            this(name, category, unit, calories, protein, fat, carb, image, from, to, Set.of());
        }
    }

    public record IngredientFormRequest(String name, String category, String unit, BigDecimal caloriesPerUnit,
                                        BigDecimal proteinPerUnit, BigDecimal fatPerUnit, BigDecimal carbPerUnit,
                                        String imageUrl, String gradientFrom, String gradientTo,
                                        MultipartFile imageFile) {
    }

    public record IngredientAliasResponse(UUID id, UUID ingredientId, String aliasName, String normalizedAliasName) {
    }

    public record CreateIngredientAliasRequest(
            @NotBlank @Size(max = 200) String aliasName) {
    }
}
