package com.zpantry.pantry.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class OllamaPantryParserTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void acceptsTheDocumentedIngredientsSchema() throws Exception {
        assertThat(OllamaPantryParser.ingredientItems("{\"ingredients\":[{\"name\":\"Trứng gà\"}]}", json))
                .hasSize(1);
    }

    @Test
    void acceptsCommonItemsAndArraySchemasWithoutCallingOllama() throws Exception {
        assertThat(OllamaPantryParser.ingredientItems("{\"items\":[{\"item\":\"Trứng gà\"}]}", json))
                .hasSize(1);
        assertThat(OllamaPantryParser.ingredientItems("[{\"name\":\"Trứng gà\"}]", json)).hasSize(1);
        assertThat(OllamaPantryParser.ingredientItems("{\"isRelevant\":false}", json)).isEmpty();
    }
}
