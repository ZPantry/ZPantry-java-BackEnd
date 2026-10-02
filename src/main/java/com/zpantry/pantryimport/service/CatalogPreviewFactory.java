package com.zpantry.pantryimport.service;

import com.zpantry.ingredient.api.IngredientDtos.IngredientResponse;
import com.zpantry.ingredient.domain.IngredientEntity;
import com.zpantry.pantryimport.api.PantryImportDtos.PantryImportPreviewItem;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The only boundary where AI-extracted food names become Pantry import rows.
 * AI values are untrusted hints: an output row exists only for one active catalog Ingredient.
 */
@Component
public class CatalogPreviewFactory {
    private final IngredientResolver resolver;

    public CatalogPreviewFactory(IngredientResolver resolver) {
        this.resolver = resolver;
    }

    public Optional<PantryImportPreviewItem> resolved(String rawName, BigDecimal suggestedQuantity,
            BigDecimal price, BigDecimal confidence) {
        if (rawName == null || rawName.isBlank()) return Optional.empty();
        var result = resolver.resolve(stripLeadingQuantity(rawName));
        IngredientEntity ingredient = result.ingredient();
        if (ingredient == null || ingredient.getId() == null || ingredient.unit == null || ingredient.unit.isBlank()) {
            return Optional.empty();
        }
        BigDecimal quantity = positive(suggestedQuantity) ? suggestedQuantity : defaultQuantity(ingredient);
        if (!positive(quantity)) return Optional.empty();
        return Optional.of(new PantryImportPreviewItem(rawName.trim(), result.normalizedName(), ingredient.getId(),
                ingredient.name, quantity, ingredient.unit, nonNegative(price), confidence,
                result.status(), ingredientResponse(ingredient)));
    }

    private static String stripLeadingQuantity(String rawName) {
        return rawName.trim().replaceFirst("^\\s*\\d+(?:[.,]\\d+)?\\s*(?:g|kg|ml|l|cái|quả|miếng|bó)?\\s+", "");
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
