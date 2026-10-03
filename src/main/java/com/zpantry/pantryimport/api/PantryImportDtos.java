package com.zpantry.pantryimport.api;

import com.zpantry.ingredient.api.IngredientDtos.IngredientResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.*;

public final class PantryImportDtos {
    private PantryImportDtos() {
    }

    public enum SourceType {TEXT, RECEIPT, FOOD_IMAGE}

    public enum ImageType {RECEIPT, FOOD_IMAGE, UNKNOWN}

    public enum ResolverStatus {RESOLVED, AMBIGUOUS, UNRESOLVED}

    public record PantryImportPreviewResponse(SourceType sourceType, List<PantryImportPreviewItem> items,
                                              List<String> warnings) {
    }

    /** V2 unified image analysis. Items are catalog-resolved server-side, never provider IDs. */
    public record UnifiedImageAnalysisResponse(ImageType imageType, BigDecimal confidence,
                                               List<PantryImportPreviewItem> ingredients,
                                               List<String> warnings) {
    }

    public record PantryImportPreviewItem(String rawName, String normalizedName, UUID ingredientId,
                                          String canonicalIngredientName, BigDecimal quantity, String unit,
                                          BigDecimal price, BigDecimal confidence, ResolverStatus resolverStatus,
                                          IngredientResponse ingredient, boolean reviewRequired, String sourceUnit) {
    }

    public record ConfirmPantryImportRequest(@NotEmpty List<@Valid ConfirmPantryImportItem> items) {
    }

    public record ConfirmPantryImportItem(@NotNull UUID ingredientId,
                                          @NotNull @DecimalMin(value = "0.0001") BigDecimal quantity,
                                          @NotBlank @Size(max = 50) String unit) {
    }
}
