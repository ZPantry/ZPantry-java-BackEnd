package com.zpantry.pantry.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PantryTextExtractionServiceTest {
    private final PantryTextExtractionService service = new PantryTextExtractionService();

    @Test
    void extractsExplicitSegmentsAndQuantitiesWithoutCallingAnyAiProvider() {
        var result = service.extract("Tôi có 500g thịt heo, 2 trứng và cà chua");

        assertThat(result).extracting(item -> item.rawName())
                .containsExactly("thịt heo", "trứng", "cà chua");
        assertThat(result.getFirst().quantity()).isEqualByComparingTo("500");
        assertThat(result.get(1).quantity()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(result.get(2).quantity()).isNull();
    }
}
