package com.zpantry.recommendation.service;

import com.zpantry.ingredient.persistence.IngredientRepository;
import com.zpantry.integration.ai.AiClient;
import com.zpantry.pantry.persistence.PantryItemRepository;
import com.zpantry.recipe.persistence.RecipeIngredientRepository;
import com.zpantry.recipe.persistence.RecipeRepository;
import com.zpantry.user.domain.FoodAllergen;
import com.zpantry.user.persistence.UserProfileRepository;
import com.zpantry.recommendation.api.RecommendationDtos.PersonalizedRecommendationRequest;
import com.zpantry.recommendation.api.RecommendationDtos.RecommendationMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
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

    public Map<String, Object> recommend(UUID userId, PersonalizedRecommendationRequest input) {
        int topK = input == null || input.topK() == null ? 5 : Math.max(1, Math.min(input.topK(), 20));
        var profile = profiles.findByUserIdAndDeletedFalse(userId).orElse(null);
        var blocked = profile == null ? Set.<FoodAllergen>of() : allergens(profile.getAllergies());
        var pantryItems = pantry.findAllByUserIdAndDeletedFalseOrderByExpiredAtAscCreatedAtAsc(userId);
        var available = pantryItems.stream().map(item -> ingredients.findByIdAndDeletedFalse(item.ingredientId).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        RecommendationMode requestedMode = input == null || input.mode() == null ? RecommendationMode.AUTO : input.mode();
        RecommendationMode mode = requestedMode == RecommendationMode.AUTO
                ? (available.isEmpty() ? RecommendationMode.PROFILE_BASED : RecommendationMode.PANTRY_BASED) : requestedMode;
        var availableIds = available.stream().map(ingredient -> ingredient.getId()).collect(java.util.stream.Collectors.toSet());
        var candidates = recipes.findAllByDeletedFalse(Pageable.unpaged()).getContent().stream()
                .filter(recipe -> Collections.disjoint(allergens(recipe.allergens), blocked))
                .filter(recipe -> input == null || input.maxCookTimeMinutes() == null || recipe.cookingTimeMinutes == null
                        || recipe.cookingTimeMinutes <= input.maxCookTimeMinutes())
                .map(recipe -> candidate(recipe.getId(), recipe.name, recipe.instructionText, availableIds))
                .sorted(java.util.Comparator.<Map<String, Object>>comparingInt(candidate -> -((Number) candidate.get("pantryMatchCount")).intValue())
                        .thenComparing(candidate -> (String) candidate.get("recipeName")))
                .limit(15)
                .toList();

        String requestId = UUID.randomUUID().toString();
        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("contractVersion", "1");
        request.put("mode", mode.name());
        request.put("profile", profilePayload(profile, input));
        request.put("pantryIngredients", pantryItems.stream().map(pantryItem -> {
            var ingredient = ingredients.findByIdAndDeletedFalse(pantryItem.ingredientId).orElse(null);
            if (ingredient == null) return null;
            Map<String, Object> pantryIngredient = new HashMap<>();
            pantryIngredient.put("ingredientId", ingredient.getId().toString()); pantryIngredient.put("name", ingredient.name);
            pantryIngredient.put("expiringSoon", pantryItem.expiredAt != null && pantryItem.expiredAt.isBefore(java.time.Instant.now().plus(java.time.Duration.ofDays(3))));
            return pantryIngredient;
        }).filter(Objects::nonNull).toList());
        request.put("candidateRecipes", candidates.stream().map(candidate -> {
            Map<String, Object> payloadCandidate = new HashMap<>(candidate);
            payloadCandidate.remove("pantryMatchCount"); // Java-only ordering signal; rejected by the strict Python contract.
            return payloadCandidate;
        }).toList());
        request.put("topK", topK);
        request.put("withAdvice", true);
        return ai.post("/ai/recommend-meals/v2", request);
    }

    private Map<String, Object> candidate(UUID recipeId, String recipeName, String instructionText, Set<UUID> availableIds) {
        var links = recipeIngredients.findAllByRecipeIdAndDeletedFalse(recipeId);
        var ingredientNames = links.stream()
                .map(link -> ingredients.findByIdAndDeletedFalse(link.ingredientId).orElse(null))
                .filter(Objects::nonNull)
                .map(ingredient -> ingredient.name)
                .toList();
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("recipeId", recipeId.toString());
        candidate.put("recipeName", recipeName);
        long matches = links.stream().map(link -> link.ingredientId).filter(availableIds::contains).count();
        candidate.put("mainIngredients", ingredientNames);
        candidate.put("kcalPerServing", null); candidate.put("proteinG", null);
        candidate.put("cookTimeMinutes", null);
        candidate.put("pantryMatchRatio", links.isEmpty() ? 0.0 : (double) matches / links.size());
        candidate.put("missingIngredients", links.stream().filter(link -> !availableIds.contains(link.ingredientId))
                .map(link -> ingredients.findByIdAndDeletedFalse(link.ingredientId).orElse(null)).filter(Objects::nonNull).map(i -> i.name).toList());
        candidate.put("expiringSoonUsed", false);
        candidate.put("descriptionSnippet", instructionText == null ? "" : instructionText.substring(0, Math.min(300, instructionText.length())));
        candidate.put("pantryMatchCount", matches);
        return candidate;
    }

    private Map<String, Object> profilePayload(com.zpantry.user.domain.UserProfileEntity profile,
                                                PersonalizedRecommendationRequest input) {
        Map<String, Object> result = new HashMap<>();
        result.put("targetKcalPerMeal", profile == null || profile.getDailyCalorieTarget() == null ? null
                : profile.getDailyCalorieTarget().divide(java.math.BigDecimal.valueOf(3), 2, java.math.RoundingMode.HALF_UP));
        result.put("proteinGPerMeal", profile == null || profile.getDailyProteinTarget() == null ? null
                : profile.getDailyProteinTarget().divide(java.math.BigDecimal.valueOf(3), 2, java.math.RoundingMode.HALF_UP));
        result.put("diet", profile == null ? "DIVERSE" : profile.getDietPreference());
        result.put("goals", profile == null || profile.getGoals() == null ? java.util.List.of() : java.util.List.of(profile.getGoals().split(",")));
        result.put("maxCookTimeMinutes", input != null && input.maxCookTimeMinutes() != null ? input.maxCookTimeMinutes()
                : profile != null && profile.getGoals() != null && profile.getGoals().contains("QUICK_COOKING") ? 20 : null);
        return result;
    }

    private static Set<FoodAllergen> allergens(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split(",")).filter(item -> !item.isBlank()).map(FoodAllergen::valueOf)
                .collect(java.util.stream.Collectors.toSet());
    }
}
