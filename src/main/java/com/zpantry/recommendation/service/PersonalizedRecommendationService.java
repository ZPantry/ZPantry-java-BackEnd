package com.zpantry.recommendation.service;

import com.zpantry.ingredient.persistence.IngredientRepository;
import com.zpantry.integration.ai.AiClient;
import com.zpantry.pantry.persistence.PantryItemRepository;
import com.zpantry.recipe.persistence.RecipeIngredientRepository;
import com.zpantry.recipe.persistence.RecipeRepository;
import com.zpantry.user.domain.FoodAllergen;
import com.zpantry.user.persistence.UserProfileRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class PersonalizedRecommendationService {
    private final UserProfileRepository profiles;
    private final PantryItemRepository pantry;
    private final IngredientRepository ingredients;
    private final RecipeRepository recipes;
    private final RecipeIngredientRepository recipeIngredients;
    private final AiClient ai;

    public PersonalizedRecommendationService(UserProfileRepository profiles, PantryItemRepository pantry,
            IngredientRepository ingredients, RecipeRepository recipes, RecipeIngredientRepository recipeIngredients,
            AiClient ai) {
        this.profiles = profiles;
        this.pantry = pantry;
        this.ingredients = ingredients;
        this.recipes = recipes;
        this.recipeIngredients = recipeIngredients;
        this.ai = ai;
    }

    public Map<String, Object> recommend(UUID userId, int topK) {
        var profile = profiles.findByUserIdAndDeletedFalse(userId).orElse(null);
        var blocked = profile == null ? Set.<FoodAllergen>of() : allergens(profile.getAllergies());
        var available = pantry.findAllByUserIdAndDeletedFalseOrderByExpiredAtAscCreatedAtAsc(userId).stream()
                .map(item -> ingredients.findByIdAndDeletedFalse(item.ingredientId).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        var candidates = recipes.findAllByDeletedFalse(PageRequest.of(0, 100)).stream()
                .filter(recipe -> Collections.disjoint(allergens(recipe.allergens), blocked))
                .map(recipe -> candidate(recipe.getId(), recipe.name, recipe.instructionText))
                .toList();

        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId.toString());
        request.put("inputIngredientText", String.join(",", available.stream().map(ingredient -> ingredient.name).toList()));
        request.put("ingredients", available.stream().map(ingredient -> {
            Map<String, Object> item = new HashMap<>();
            item.put("ingredientId", ingredient.getId().toString());
            item.put("name", ingredient.name);
            item.put("quantity", 0);
            item.put("unit", ingredient.unit == null ? "" : ingredient.unit);
            return item;
        }).toList());
        request.put("candidateRecipes", candidates);
        request.put("topK", Math.max(1, Math.min(topK, 20)));
        return ai.post("/ai/recommend-meals", request);
    }

    private Map<String, Object> candidate(UUID recipeId, String recipeName, String instructionText) {
        var ingredientNames = recipeIngredients.findAllByRecipeIdAndDeletedFalse(recipeId).stream()
                .map(link -> ingredients.findByIdAndDeletedFalse(link.ingredientId).orElse(null))
                .filter(Objects::nonNull)
                .map(ingredient -> ingredient.name)
                .toList();
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("recipeId", recipeId.toString());
        candidate.put("recipeName", recipeName);
        candidate.put("ingredientNames", ingredientNames);
        candidate.put("instructionText", instructionText == null ? "" : instructionText);
        return candidate;
    }

    private static Set<FoodAllergen> allergens(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split(",")).filter(item -> !item.isBlank()).map(FoodAllergen::valueOf)
                .collect(java.util.stream.Collectors.toSet());
    }
}
