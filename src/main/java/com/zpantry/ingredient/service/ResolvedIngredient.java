package com.zpantry.ingredient.service;

import com.zpantry.ingredient.domain.IngredientEntity;
import com.zpantry.pantryimport.api.PantryImportDtos.ResolverStatus;
import java.util.List;

/** Result of resolving an untrusted food label to one active canonical Ingredient. */
public record ResolvedIngredient(String normalizedInput, IngredientEntity canonicalFood,
                                 ResolverStatus status, String matchedName, List<Candidate> candidates) {
    public record Candidate(IngredientEntity ingredient, double score) { }
    public boolean isResolved() {
        return status == ResolverStatus.RESOLVED && canonicalFood != null;
    }

    static ResolvedIngredient resolved(String input, IngredientEntity canonicalFood, String matchedName) {
        return new ResolvedIngredient(input, canonicalFood, ResolverStatus.RESOLVED, matchedName, List.of());
    }

    static ResolvedIngredient unresolved(String input, ResolverStatus status, List<Candidate> candidates) {
        return new ResolvedIngredient(input, null, status, null, List.copyOf(candidates));
    }
}
