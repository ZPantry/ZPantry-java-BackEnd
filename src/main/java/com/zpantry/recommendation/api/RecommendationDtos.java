package com.zpantry.recommendation.api;

import java.math.BigDecimal;
import java.util.*;

public final class RecommendationDtos {
    private RecommendationDtos() {
    }

    public record RecommendMealIngredientRequest(UUID ingredientId, String name, BigDecimal quantity, String unit) {
    }

    public record RecommendMealCandidateRecipeRequest(UUID recipeId, String recipeName, List<String> ingredientNames,
                                                      String instructionText) {
    }

    public record RecommendMealRequest(String inputIngredientText, List<String> ingredients,
                                       List<RecommendMealIngredientRequest> selectedIngredients,
                                       List<RecommendMealCandidateRecipeRequest> candidateRecipes, int topK) {
    }

    public record RecommendationFeedbackRequest(UUID mealRecommendationId, UUID recipeId, int rating,
                                                String feedbackType, String comment) {
    }

    public enum RecommendationMode { AUTO, PANTRY_BASED, PROFILE_BASED }

    public record PersonalizedRecommendationRequest(Integer topK, RecommendationMode mode,
                                                    String mealType, Integer maxCookTimeMinutes,
                                                    Integer servings, Boolean includeIngredients) {
    }
}
