package com.zpantry.pantryimport.service;

import com.zpantry.ingredient.service.FoodNameNormalizer;
import java.math.BigDecimal;
import java.util.Map;

/** Atomic quantity/unit conversion. Never relabels a quantity without converting it. */
public final class QuantityUnitNormalizer {
    private static final Map<String, Unit> UNITS = Map.ofEntries(
            Map.entry("g", new Unit("g", BigDecimal.ONE)), Map.entry("gr", new Unit("g", BigDecimal.ONE)),
            Map.entry("gam", new Unit("g", BigDecimal.ONE)), Map.entry("kg", new Unit("g", BigDecimal.valueOf(1000))),
            Map.entry("kilogram", new Unit("g", BigDecimal.valueOf(1000))),
            Map.entry("ml", new Unit("ml", BigDecimal.ONE)), Map.entry("l", new Unit("ml", BigDecimal.valueOf(1000))),
            Map.entry("lit", new Unit("ml", BigDecimal.valueOf(1000))), Map.entry("litre", new Unit("ml", BigDecimal.valueOf(1000))),
            Map.entry("liters", new Unit("ml", BigDecimal.valueOf(1000))), Map.entry("lít", new Unit("ml", BigDecimal.valueOf(1000))),
            Map.entry("piece", new Unit("count", BigDecimal.ONE)), Map.entry("pieces", new Unit("count", BigDecimal.ONE)),
            Map.entry("cai", new Unit("count", BigDecimal.ONE)), Map.entry("qua", new Unit("count", BigDecimal.ONE)),
            Map.entry("bo", new Unit("count", BigDecimal.ONE)), Map.entry("hop", new Unit("count", BigDecimal.ONE)),
            Map.entry("chai", new Unit("count", BigDecimal.ONE)));
    private QuantityUnitNormalizer() { }
    public static Result normalize(BigDecimal quantity, String suppliedUnit, String canonicalUnit) {
        if (quantity == null) return new Result(null, canonicalUnit, false);
        if (quantity.signum() <= 0) return new Result(null, canonicalUnit, true);
        Unit source = UNITS.get(FoodNameNormalizer.normalize(suppliedUnit));
        String canonical = FoodNameNormalizer.normalize(canonicalUnit);
        boolean compatible = source != null && (source.canonical().equals(canonical)
                || (source.canonical().equals("count") && java.util.Set.of("cai", "qua", "bo", "hop", "chai").contains(canonical)));
        if (!compatible)
            return new Result(null, canonicalUnit, true);
        return new Result(quantity.multiply(source.factor()).stripTrailingZeros(), canonicalUnit, false);
    }
    public record Result(BigDecimal quantity, String unit, boolean reviewRequired) { }
    private record Unit(String canonical, BigDecimal factor) { }
}
