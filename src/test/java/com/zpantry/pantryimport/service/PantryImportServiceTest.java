package com.zpantry.pantryimport.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zpantry.integration.ai.AiClient;
import com.zpantry.pantry.service.PantryService;
import com.zpantry.pantryimport.api.PantryImportDtos.ConfirmPantryImportItem;
import com.zpantry.pantryimport.api.PantryImportDtos.ConfirmPantryImportRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

class PantryImportServiceTest {
    @Test
    void confirmAddsQuantityOnceWhenTheClientRetriesTheSameKey() {
        PantryService pantry = Mockito.mock(PantryService.class);
        JdbcTemplate jdbc = Mockito.mock(JdbcTemplate.class);
        when(jdbc.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);
        PantryImportService service = new PantryImportService(Mockito.mock(AiClient.class),
                Mockito.mock(PantryIngredientPipeline.class), pantry, jdbc);
        UUID userId = UUID.randomUUID();
        UUID ingredientId = UUID.randomUUID();
        var request = new ConfirmPantryImportRequest(null,
                List.of(new ConfirmPantryImportItem(ingredientId, new BigDecimal("1.5"), "kg")));

        service.confirm(userId, request, "retry-key");

        verify(pantry).validateImportItem(userId, ingredientId, new BigDecimal("1.5"), "kg");
        verify(pantry).addFromImport(userId, ingredientId, new BigDecimal("1.5"), "kg");
    }

    @Test
    void confirmDoesNotMutatePantryWhenTheIdempotencyKeyAlreadyExists() {
        PantryService pantry = Mockito.mock(PantryService.class);
        JdbcTemplate jdbc = Mockito.mock(JdbcTemplate.class);
        when(jdbc.update(anyString(), any(), any(), any(), any(), any())).thenReturn(0);
        PantryImportService service = new PantryImportService(Mockito.mock(AiClient.class),
                Mockito.mock(PantryIngredientPipeline.class), pantry, jdbc);
        var request = new ConfirmPantryImportRequest(null,
                List.of(new ConfirmPantryImportItem(UUID.randomUUID(), BigDecimal.ONE, "g")));

        UUID userId = UUID.randomUUID();
        service.confirm(userId, request, "retry-key");

        verify(pantry, never()).validateImportItem(any(), any(), any(), any());
        verify(pantry, never()).addFromImport(any(), any(), any(), any());
    }
}
