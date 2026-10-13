package com.zpantry.user.service;

import com.zpantry.ingredient.persistence.IngredientRepository;
import com.zpantry.recipe.persistence.RecipeIngredientRepository;
import com.zpantry.user.domain.FoodAllergen;
import com.zpantry.user.persistence.UserProfileRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Enforces each user's saved allergy profile at write boundaries. */
@Service
public class AllergenSafetyService {
    private final UserProfileRepository profiles;
    private final IngredientRepository ingredients;
    private final RecipeIngredientRepository recipeIngredients;

    public AllergenSafetyService(UserProfileRepository profiles, IngredientRepository ingredients,
                                 RecipeIngredientRepository recipeIngredients) {
        this.profiles = profiles;
        this.ingredients = ingredients;
        this.recipeIngredients = recipeIngredients;
    }

    public String ingredientConflict(UUID userId, Collection<UUID> ingredientIds) {
        Set<FoodAllergen> blocked = userAllergies(userId);
        if (blocked.isEmpty() || ingredientIds == null || ingredientIds.isEmpty()) return null;

        List<String> conflicts = new ArrayList<>();
        for (UUID ingredientId : ingredientIds) {
            ingredients.findByIdAndDeletedFalse(ingredientId).ifPresent(ingredient -> {
                Set<FoodAllergen> matches = matching(blocked, ingredient.allergens);
                if (!matches.isEmpty()) conflicts.add(ingredient.name + " (" + labels(matches) + ")");
            });
        }
        return conflicts.isEmpty() ? null : "Không thể thêm vì có chất gây dị ứng đã khai báo: " + String.join(", ", conflicts) + ".";
    }

    public String recipeConflict(UUID userId, UUID recipeId) {
        if (recipeId == null) return "Không tìm thấy công thức của món ăn.";
        List<UUID> requiredIngredients = recipeIngredients.findAllByRecipeIdAndDeletedFalse(recipeId).stream()
                .filter(link -> link.required).map(link -> link.ingredientId).toList();
        String conflict = ingredientConflict(userId, requiredIngredients);
        return conflict == null ? null : conflict.replace("Không thể thêm", "Không thể thêm món vào thực đơn");
    }

    private Set<FoodAllergen> userAllergies(UUID userId) {
        return profiles.findByUserIdAndDeletedFalse(userId)
                .map(profile -> FoodAllergen.parseStoredSet(profile.getAllergies()))
                .orElseGet(Set::of);
    }

    private static Set<FoodAllergen> matching(Set<FoodAllergen> blocked, String ingredientAllergens) {
        Set<FoodAllergen> matches = EnumSet.noneOf(FoodAllergen.class);
        for (FoodAllergen allergen : FoodAllergen.parseStoredSet(ingredientAllergens)) {
            if (blocked.contains(allergen)) matches.add(allergen);
        }
        return matches;
    }

    private static String labels(Set<FoodAllergen> allergens) {
        return allergens.stream().sorted(Comparator.comparing(Enum::name)).map(AllergenSafetyService::label).reduce((a, b) -> a + ", " + b).orElse("");
    }

    private static String label(FoodAllergen allergen) {
        return switch (allergen) {
            case PEANUT -> "đậu phộng";
            case TREE_NUT -> "hạt cây";
            case MILK -> "sữa";
            case EGG -> "trứng";
            case SOY -> "đậu nành";
            case WHEAT -> "lúa mì";
            case GLUTEN -> "gluten";
            case FISH -> "cá";
            case SHELLFISH -> "giáp xác";
            case MOLLUSCS -> "nhuyễn thể";
            case SESAME -> "mè/vừng";
            case NO_ALLERGIES -> "";
        };
    }
}
