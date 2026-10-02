package com.zpantry.ingredient.api;

import com.zpantry.user.domain.FoodAllergen;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public final class IngredientDtos {
    private IngredientDtos() {
    }

    public record IngredientResponse(UUID id, String name, String normalizedName, String category, String unit,
                                     BigDecimal caloriesPerUnit, BigDecimal proteinPerUnit, BigDecimal fatPerUnit,
                                     BigDecimal carbPerUnit, BigDecimal defaultQuantity, String imageUrl, String gradientFrom, String gradientTo,
                                     Set<FoodAllergen> allergens) {
    }

    public record CreateIngredientRequest(String name, String category, String unit, BigDecimal caloriesPerUnit,
                                          BigDecimal protenPerUnit, BigDecimal fatPerUnit, BigDecimal carbPerUnit,
                                          String imageUrl, String gradientFrom, String gradientTo,
                                          Set<FoodAllergen> allergens) {
        public CreateIngredientRequest(String name, String category, String unit, BigDecimal calories,
                BigDecimal protein, BigDecimal fat, BigDecimal carb, String image, String from, String to) {
            this(name, category, unit, calories, protein, fat, carb, image, from, to, Set.of());
        }
    }

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
}
