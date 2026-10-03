package com.zpantry.pantryimport.domain;

import java.math.BigDecimal;

/** Untrusted ingredient data extracted from text or an image provider. */
public record ExtractedIngredient(String rawName, BigDecimal quantity, String unit, BigDecimal price, BigDecimal confidence) {
    public ExtractedIngredient(String rawName, BigDecimal quantity, BigDecimal price, BigDecimal confidence) {
        this(rawName, quantity, null, price, confidence);
    }
}
