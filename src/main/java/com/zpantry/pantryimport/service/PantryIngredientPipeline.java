package com.zpantry.pantryimport.service;

import com.zpantry.ingredient.api.IngredientDtos.IngredientResponse;
import com.zpantry.ingredient.domain.IngredientEntity;
import com.zpantry.ingredient.service.FoodMatchingService;
import com.zpantry.pantryimport.api.PantryImportDtos.PantryImportPreviewItem;
import com.zpantry.pantryimport.domain.ExtractedIngredient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Shared finalization path for text, receipt and food-photo extraction. Only a resolved
 * active canonical Ingredient can produce a preview or subsequently be persisted to a pantry.
 */
@Component
public class PantryIngredientPipeline {
    private final FoodMatchingService matching;

    public PantryIngredientPipeline(FoodMatchingService matching) {
        this.matching = matching;
    }

    public Optional<PantryImportPreviewItem> resolve(ExtractedIngredient extracted) {
        if (extracted == null || extracted.rawName() == null || extracted.rawName().isBlank()) return Optional.empty();
        var resolved = matching.resolve(extracted.rawName());
        if (!resolved.isResolved() && extracted.canonicalHint() != null && !extracted.canonicalHint().isBlank()) {
            resolved = matching.resolve(extracted.canonicalHint());
        }
        IngredientEntity ingredient = resolved.canonicalFood();
        if (!resolved.isResolved() || ingredient.getId() == null || ingredient.unit == null || ingredient.unit.isBlank()) {
            var flags = new java.util.ArrayList<>(extracted.flags() == null ? List.<String>of() : extracted.flags());
            if (extracted.confidence() != null && extracted.confidence().compareTo(BigDecimal.valueOf(0.6)) < 0 && !flags.contains("LOW_CONFIDENCE")) flags.add("LOW_CONFIDENCE");
            if (extracted.quantity() == null && !flags.contains("NO_QUANTITY")) flags.add("NO_QUANTITY");
            return Optional.of(new PantryImportPreviewItem(extracted.rawName().trim(), resolved.normalizedInput(), null,
                    extracted.canonicalHint(), extracted.quantity(), extracted.unit(), nonNegative(extracted.price()), extracted.confidence(),
                    resolved.status(), null, true, extracted.unit(), List.copyOf(flags), candidates(resolved)));
        }
        var normalized = QuantityUnitNormalizer.normalize(extracted.quantity(), extracted.unit(), ingredient.unit);
        BigDecimal quantity = normalized.quantity() == null ? defaultQuantity(ingredient) : normalized.quantity();
        if (!positive(quantity)) return Optional.empty();
        var flags = new java.util.ArrayList<>(extracted.flags() == null ? List.<String>of() : extracted.flags());
        if (extracted.confidence() != null && extracted.confidence().compareTo(BigDecimal.valueOf(0.6)) < 0) flags.add("LOW_CONFIDENCE");
        if (extracted.quantity() == null) flags.add("NO_QUANTITY");
        if (normalized.reviewRequired()) flags.add("ANOMALY");
        return Optional.of(new PantryImportPreviewItem(extracted.rawName().trim(), resolved.normalizedInput(), ingredient.getId(),
                ingredient.name, quantity, ingredient.unit, nonNegative(extracted.price()), extracted.confidence(),
                resolved.status(), ingredientResponse(ingredient), normalized.reviewRequired(), extracted.unit(), List.copyOf(flags), List.of()));
    }

    private static List<com.zpantry.pantryimport.api.PantryImportDtos.IngredientCandidate> candidates(com.zpantry.ingredient.service.ResolvedIngredient resolved) {
        return resolved.candidates().stream().map(candidate -> new com.zpantry.pantryimport.api.PantryImportDtos.IngredientCandidate(
                candidate.ingredient().getId(), candidate.ingredient().name, candidate.ingredient().unit,
                BigDecimal.valueOf(candidate.score()))).toList();
    }

    private static BigDecimal defaultQuantity(IngredientEntity ingredient) {
        return positive(ingredient.defaultQuantity) ? ingredient.defaultQuantity : BigDecimal.ONE;
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        return value != null && value.signum() >= 0 ? value : null;
    }

    private static IngredientResponse ingredientResponse(IngredientEntity ingredient) {
        return new IngredientResponse(ingredient.getId(), ingredient.name, ingredient.normalizedName,
                ingredient.category, ingredient.unit, ingredient.caloriesPerUnit, ingredient.proteinPerUnit,
                ingredient.fatPerUnit, ingredient.carbPerUnit, ingredient.defaultQuantity, ingredient.imageUrl,
                ingredient.gradientFrom, ingredient.gradientTo, Set.of());
    }
}
