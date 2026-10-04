package com.zpantry.recommendation.service;

import com.zpantry.ingredient.persistence.IngredientRepository;
import com.zpantry.pantry.persistence.PantryItemRepository;
import com.zpantry.recipe.persistence.RecipeIngredientRepository;
import com.zpantry.recipe.persistence.RecipeRepository;
import com.zpantry.user.domain.FoodAllergen;
import com.zpantry.user.persistence.UserProfileRepository;
import com.zpantry.recommendation.api.RecommendationDtos.PersonalizedRecommendationRequest;
import com.zpantry.recommendation.api.RecommendationDtos.PersonalizedRecommendationResponse;
import com.zpantry.recommendation.api.RecommendationDtos.RankedMealRecommendationResponse;
import com.zpantry.recommendation.api.RecommendationDtos.RecommendationMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalizedRecommendationService {
    private final UserProfileRepository profiles;
    private final PantryItemRepository pantry;
    private final IngredientRepository ingredients;
    private final RecipeRepository recipes;
    private final RecipeIngredientRepository recipeIngredients;
    private final MealRecommendationRanker ranker;

    public PersonalizedRecommendationService(UserProfileRepository profiles, PantryItemRepository pantry,
            IngredientRepository ingredients, RecipeRepository recipes, RecipeIngredientRepository recipeIngredients) {
        this.profiles = profiles;
        this.pantry = pantry;
        this.ingredients = ingredients;
        this.recipes = recipes;
        this.recipeIngredients = recipeIngredients;
        this.ranker = new MealRecommendationRanker();
    }

    @Transactional(readOnly = true)
    public PersonalizedRecommendationResponse recommend(UUID userId, PersonalizedRecommendationRequest input) {
        int topK = input == null || input.topK() == null ? 10 : Math.max(1, Math.min(input.topK(), 10));
        var profile = profiles.findByUserIdAndDeletedFalse(userId).orElse(null);
        var blocked = profile == null ? Set.<FoodAllergen>of() : allergens(profile.getAllergies());
        var pantryItems = pantry.findAllByUserIdAndDeletedFalseOrderByExpiredAtAscCreatedAtAsc(userId);
        var pantryIngredients = pantryItems.stream().map(item -> new PantryIngredient(item,
                        ingredients.findByIdAndDeletedFalse(item.ingredientId).orElse(null)))
                .filter(item -> item.ingredient() != null).toList();
        RecommendationMode requestedMode = input == null || input.mode() == null ? RecommendationMode.AUTO : input.mode();
        RecommendationMode mode = requestedMode == RecommendationMode.AUTO
                ? (pantryIngredients.isEmpty() ? RecommendationMode.PROFILE_BASED : RecommendationMode.PANTRY_BASED) : requestedMode;
        var availableIds = pantryIngredients.stream().map(item -> item.ingredient().getId()).collect(java.util.stream.Collectors.toSet());
        var expiringSoonIds = pantryIngredients.stream().filter(item -> isExpiringSoon(item.pantryItem()))
                .map(item -> item.ingredient().getId()).collect(java.util.stream.Collectors.toSet());
        var candidates = recipes.findAllByDeletedFalse(Pageable.unpaged()).getContent().stream()
                .filter(recipe -> Collections.disjoint(allergens(recipe.allergens), blocked))
                .filter(recipe -> input == null || input.maxCookTimeMinutes() == null || recipe.cookingTimeMinutes == null
                        || recipe.cookingTimeMinutes <= input.maxCookTimeMinutes())
                .map(this::candidate)
                .toList();
        var ranked = ranker.rank(candidates, availableIds, expiringSoonIds, topK).stream()
                .map(this::response).toList();
        return new PersonalizedRecommendationResponse(mode, ranked);
    }

    private MealRecommendationRanker.RecipeCandidate candidate(com.zpantry.recipe.domain.RecipeEntity recipe) {
        var links = recipeIngredients.findAllByRecipeIdAndDeletedFalse(recipe.getId());
        var recipeItems = links.stream()
                .map(link -> {
                    var ingredient = ingredients.findByIdAndDeletedFalse(link.ingredientId).orElse(null);
                    return ingredient == null ? null : new MealRecommendationRanker.RecipeIngredient(
                            ingredient.getId(), ingredient.name, link.required);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        return new MealRecommendationRanker.RecipeCandidate(recipe.getId(), recipe.name, recipe.cookingTimeMinutes, recipeItems);
    }

    private RankedMealRecommendationResponse response(MealRecommendationRanker.RankedRecipe ranked) {
        var reasons = new java.util.ArrayList<String>();
        if (!ranked.matchingIngredients().isEmpty()) reasons.add("Uses " + ranked.matchingIngredients().size() + " pantry ingredients");
        if (!ranked.expiringSoonIngredients().isEmpty()) reasons.add("Uses ingredients expiring soon");
        if (!ranked.missingIngredients().isEmpty()) reasons.add("Missing " + ranked.missingIngredients().size() + " ingredients");
        if (reasons.isEmpty()) reasons.add("Matches your saved cooking constraints");
        return new RankedMealRecommendationResponse(ranked.recipeId(), ranked.recipeName(), ranked.cookingTimeMinutes(),
                ranked.score(), ranked.pantryMatchRatio(), ranked.matchingIngredients(), ranked.missingIngredients(),
                ranked.expiringSoonIngredients(), reasons);
    }

    private static boolean isExpiringSoon(com.zpantry.pantry.domain.PantryItemEntity item) {
        Instant now = Instant.now();
        return item.expiredAt != null && !item.expiredAt.isBefore(now)
                && item.expiredAt.isBefore(now.plus(java.time.Duration.ofDays(3)));
    }

    private record PantryIngredient(com.zpantry.pantry.domain.PantryItemEntity pantryItem,
                                    com.zpantry.ingredient.domain.IngredientEntity ingredient) { }

    private static Set<FoodAllergen> allergens(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split(",")).filter(item -> !item.isBlank()).map(FoodAllergen::valueOf)
                .collect(java.util.stream.Collectors.toSet());
    }
}
