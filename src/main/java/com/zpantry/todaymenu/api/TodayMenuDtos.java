package com.zpantry.todaymenu.api;

import java.time.*;
import java.math.BigDecimal;
import java.util.*;

import org.springframework.web.multipart.MultipartFile;

public final class TodayMenuDtos {
    private TodayMenuDtos() {
    }

    public record CreateTodayMenuItemRequest(UUID mealId, UUID recipeId, String mealName, String mealType,
                                             Integer servingSize, LocalDate plannedDate, String note) {
    }

    public record CompleteTodayMenuItemRequest(MultipartFile imageFile, Instant cookedAt, Integer rating, String note) {
    }

    public record TodayMenuItemResponse(UUID id, UUID mealId, UUID recipeId, String mealName, String mealType,
                                        Integer servingSize, LocalDate plannedDate, String status, String note,
                                        Instant cookedAt, String imageUrl, String imagePublicId, Instant createdAt) {
    }

    public record CookingLogResponse(UUID id, UUID todayMenuItemId, UUID mealId, UUID recipeId, String mealName,
                                     String imageUrl, String imagePublicId, Instant cookedAt, Integer rating,
                                     String note, List<Object> pantryUsageLogs) {
    }

    public record TodayMenuCompletionResponse(CookingLogResponse cookingLog, List<Object> consumedIngredients,
                                              List<Object> updatedPantryItems, List<String> warnings) {
    }

    public record IngredientAvailabilityResponse(UUID todayMenuItemId, boolean sufficient,
                                                 List<IngredientAvailability> ingredients) {
    }

    public record IngredientAvailability(UUID ingredientId, String ingredientName, BigDecimal requiredQuantity,
                                         BigDecimal availableQuantity, BigDecimal missingQuantity, String unit,
                                         boolean unitMismatch) {
    }

    public record ShoppingListItemResponse(UUID id, UUID todayMenuItemId, UUID ingredientId,
                                           String ingredientName, BigDecimal quantity, String unit,
                                           String status, Instant createdAt) {
    }
}
