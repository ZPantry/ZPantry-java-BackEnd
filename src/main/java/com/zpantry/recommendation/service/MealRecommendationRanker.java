package com.zpantry.recommendation.service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Deterministic, side-effect-free ranking policy for catalog recipes.
 *
 * <p>The ranker deliberately has no dependency on an AI provider. This keeps the default meal
 * recommendation route available during provider outages and makes its ordering explainable.</p>
 */
public final class MealRecommendationRanker {
    private static final int MATCH_SCORE = 30;
    private static final int MISSING_PENALTY = 10;
    private static final int EXPIRING_SOON_BONUS = 20;

    public List<RankedRecipe> rank(List<RecipeCandidate> candidates, Set<UUID> availableIngredientIds,
                                   Set<UUID> expiringSoonIngredientIds, int limit) {
        return candidates.stream()
                .map(candidate -> score(candidate, availableIngredientIds, expiringSoonIngredientIds))
                .sorted(Comparator.comparingInt(RankedRecipe::score).reversed()
                        .thenComparing(Comparator.comparingInt(RankedRecipe::matchedIngredientCount).reversed())
                        .thenComparing(RankedRecipe::recipeName, Comparator.nullsLast(String::compareTo)))
                .limit(limit)
                .toList();
    }

    private RankedRecipe score(RecipeCandidate candidate, Set<UUID> available, Set<UUID> expiringSoon) {
        var required = candidate.ingredients().stream().filter(RecipeIngredient::required).toList();
        var evaluated = required.isEmpty() ? candidate.ingredients() : required;
        var matched = evaluated.stream().filter(item -> available.contains(item.ingredientId())).toList();
        var missing = evaluated.stream().filter(item -> !available.contains(item.ingredientId())).toList();
        var expiringUsed = matched.stream().filter(item -> expiringSoon.contains(item.ingredientId())).toList();
        int score = matched.size() * MATCH_SCORE - missing.size() * MISSING_PENALTY
                + expiringUsed.size() * EXPIRING_SOON_BONUS;
        return new RankedRecipe(candidate.recipeId(), candidate.recipeName(), candidate.cookingTimeMinutes(), score,
                matched.size(), evaluated.size(), names(matched), names(missing), names(expiringUsed));
    }

    private static List<String> names(List<RecipeIngredient> ingredients) {
        return ingredients.stream().map(RecipeIngredient::ingredientName).toList();
    }

    public record RecipeCandidate(UUID recipeId, String recipeName, Integer cookingTimeMinutes,
                                  List<RecipeIngredient> ingredients) {
    }

    public record RecipeIngredient(UUID ingredientId, String ingredientName, boolean required) {
    }

    public record RankedRecipe(UUID recipeId, String recipeName, Integer cookingTimeMinutes, int score,
                               int matchedIngredientCount, int totalRequiredIngredientCount,
                               List<String> matchingIngredients, List<String> missingIngredients,
                               List<String> expiringSoonIngredients) {
        public double pantryMatchRatio() {
            return totalRequiredIngredientCount == 0 ? 0.0
                    : (double) matchedIngredientCount / totalRequiredIngredientCount;
        }
    }
}
