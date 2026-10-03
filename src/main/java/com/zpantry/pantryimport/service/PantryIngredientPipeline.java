package com.zpantry.pantryimport.service;

import com.zpantry.ingredient.api.IngredientDtos.IngredientResponse;
import com.zpantry.ingredient.domain.IngredientEntity;
import com.zpantry.ingredient.service.FoodMatchingService;
import com.zpantry.pantryimport.api.PantryImportDtos.PantryImportPreviewItem;
import com.zpantry.pantryimport.domain.ExtractedIngredient;
import java.math.BigDecimal;
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
        IngredientEntity ingredient = resolved.canonicalFood();
        if (!resolved.isResolved() || ingredient.getId() == null || ingredient.unit == null || ingredient.unit.isBlank()) {
            return Optional.empty();
        }
        var normalized = QuantityUnitNormalizer.normalize(extracted.quantity(), extracted.unit(), ingredient.unit);
        BigDecimal quantity = normalized.quantity() == null ? defaultQuantity(ingredient) : normalized.quantity();
        if (!positive(quantity)) return Optional.empty();
        return Optional.of(new PantryImportPreviewItem(extracted.rawName().trim(), resolved.normalizedInput(), ingredient.getId(),
                ingredient.name, quantity, ingredient.unit, nonNegative(extracted.price()), extracted.confidence(),
                resolved.status(), ingredientResponse(ingredient), normalized.reviewRequired(), extracted.unit()));
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
