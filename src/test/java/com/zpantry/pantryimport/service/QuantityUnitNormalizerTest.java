package com.zpantry.pantryimport.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class QuantityUnitNormalizerTest {

    @ParameterizedTest
    @MethodSource("massAndVolumeCases")
    void convertsKnownMassAndVolumeUnitsAtomically(String input, String sourceUnit,
                                                    String canonicalUnit, String expected) {
        var result = QuantityUnitNormalizer.normalize(new BigDecimal(input), sourceUnit, canonicalUnit);

        assertThat(result.quantity()).isEqualByComparingTo(expected);
        assertThat(result.unit()).isEqualTo(canonicalUnit);
        assertThat(result.reviewRequired()).isFalse();
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> massAndVolumeCases() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("0.5", "kg", "g", "500"),
                org.junit.jupiter.params.provider.Arguments.of("2", "KG", "g", "2000"),
                org.junit.jupiter.params.provider.Arguments.of("1.5", "lít", "ml", "1500"),
                org.junit.jupiter.params.provider.Arguments.of("250", "gr", "g", "250"));
    }

    @Test
    void preservesCountUnitsOnlyWhenTheFoodUsesACompatibleCanonicalCountUnit() {
        var result = QuantityUnitNormalizer.normalize(new BigDecimal("3"), "pieces", "quả");

        assertThat(result.quantity()).isEqualByComparingTo("3");
        assertThat(result.reviewRequired()).isFalse();
    }

    @Test
    void marksUnknownOrUnsafeUnitsForReviewRatherThanInventingAGramValue() {
        var unknown = QuantityUnitNormalizer.normalize(new BigDecimal("2"), "packet", "g");
        var incompatible = QuantityUnitNormalizer.normalize(new BigDecimal("3"), "quả", "g");
        var zero = QuantityUnitNormalizer.normalize(BigDecimal.ZERO, "g", "g");

        assertThat(unknown.quantity()).isNull();
        assertThat(unknown.reviewRequired()).isTrue();
        assertThat(incompatible.quantity()).isNull();
        assertThat(incompatible.reviewRequired()).isTrue();
        assertThat(zero.quantity()).isNull();
        assertThat(zero.reviewRequired()).isTrue();
    }
}
