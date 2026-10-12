package com.zpantry.user.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public enum FoodAllergen {
    NO_ALLERGIES,
    PEANUT, TREE_NUT, MILK, EGG, SOY, WHEAT, GLUTEN, FISH, SHELLFISH, MOLLUSCS, SESAME;

    /**
     * Reads the canonical comma-separated form and the historical catalog form, which used
     * semicolons and the plural TREE_NUTS label.
     */
    public static Set<FoodAllergen> parseStoredSet(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split("[,;]"))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .map(FoodAllergen::fromStoredCode)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static FoodAllergen fromStoredCode(String code) {
        return switch (code.trim().toUpperCase(Locale.ROOT)) {
            case "TREE_NUTS" -> TREE_NUT;
            default -> valueOf(code.trim().toUpperCase(Locale.ROOT));
        };
    }
}
