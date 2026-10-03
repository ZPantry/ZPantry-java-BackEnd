package com.zpantry.ingredient.service;

import com.zpantry.ingredient.domain.IngredientEntity;
import com.zpantry.pantryimport.api.PantryImportDtos.ResolverStatus;

/** Result of resolving an untrusted food label to one active canonical Ingredient. */
public record ResolvedIngredient(String normalizedInput, IngredientEntity canonicalFood,
                                 ResolverStatus status, String matchedName) {
    public boolean isResolved() {
        return status == ResolverStatus.RESOLVED && canonicalFood != null;
    }

    static ResolvedIngredient resolved(String input, IngredientEntity canonicalFood, String matchedName) {
        return new ResolvedIngredient(input, canonicalFood, ResolverStatus.RESOLVED, matchedName);
    }

    static ResolvedIngredient unresolved(String input, ResolverStatus status) {
        return new ResolvedIngredient(input, null, status, null);
    }
}
