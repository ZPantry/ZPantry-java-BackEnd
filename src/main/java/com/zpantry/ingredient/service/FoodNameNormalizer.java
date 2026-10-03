package com.zpantry.ingredient.service;

import java.text.Normalizer;
import java.util.Locale;

/** Normalizes human-provided food labels before deterministic catalog matching. */
public final class FoodNameNormalizer {
    private FoodNameNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) return "";
        String compatibility = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC);
        String decomposed = Normalizer.normalize(compatibility, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return decomposed
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}
