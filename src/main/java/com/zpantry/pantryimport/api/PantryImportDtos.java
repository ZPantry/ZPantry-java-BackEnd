package com.zpantry.pantryimport.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.*;

public final class PantryImportDtos {
    private PantryImportDtos() {
    }

    public enum SourceType {RECEIPT, FOOD_IMAGE}

    public enum ResolverStatus {RESOLVED, AMBIGUOUS, UNRESOLVED}

    public record PantryImportPreviewResponse(SourceType sourceType, List<PantryImportPreviewItem> items,
                                              List<String> warnings) {
    }

    public record PantryImportPreviewItem(String rawName, String normalizedName, UUID ingredientId,
                                          String canonicalIngredientName, BigDecimal quantity, String unit,
                                          BigDecimal price, BigDecimal confidence, ResolverStatus resolverStatus) {
    }

    public record ConfirmPantryImportRequest(@NotEmpty List<@Valid ConfirmPantryImportItem> items) {
    }

    public record ConfirmPantryImportItem(@NotNull UUID ingredientId,
                                          @NotNull @DecimalMin(value = "0.0001") BigDecimal quantity,
                                          @NotBlank @Size(max = 50) String unit) {
    }
}
