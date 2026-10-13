package com.zpantry.pantryimport.domain;

import java.math.BigDecimal;

/** Untrusted ingredient data extracted from text or an image provider. */
public record ExtractedIngredient(String rawName, String canonicalHint, BigDecimal quantity, String unit,
                                  BigDecimal price, BigDecimal confidence, java.util.List<String> flags) {
    public ExtractedIngredient(String rawName, BigDecimal quantity, BigDecimal price, BigDecimal confidence) {
        this(rawName, null, quantity, null, price, confidence, java.util.List.of());
    }
    public ExtractedIngredient(String rawName, BigDecimal quantity, String unit, BigDecimal price, BigDecimal confidence) {
        this(rawName, null, quantity, unit, price, confidence, java.util.List.of());
    }
}
