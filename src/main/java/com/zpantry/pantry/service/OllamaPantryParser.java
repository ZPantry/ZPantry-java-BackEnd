package com.zpantry.pantry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OllamaPantryParser {
    private final RestClient client;
    private final String model;
    private final ObjectMapper json;

    public OllamaPantryParser(@Value("${zpantry.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${zpantry.ollama.model:qwen2.5:3b}") String model, ObjectMapper json) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.model = model;
        this.json = json;
    }

    public List<ParsedItem> parse(String text) {
        try {
            String prompt = "Extract Vietnamese pantry items. Return JSON only: {\\\"items\\\":[{\\\"name\\\":\\\"string\\\",\\\"quantity\\\":number|null,\\\"unit\\\":\\\"string|null\\\"}]}. Do not invent values. Text: " + text;
            Map<?, ?> reply = client.post().uri("/api/generate")
                    .body(Map.of("model", model, "prompt", prompt, "stream", false, "format", "json"))
                    .retrieve().body(Map.class);
            Object response = reply == null ? null : reply.get("response");
            if (!(response instanceof String body)) throw new IllegalArgumentException("Ollama returned no JSON response");
            JsonNode items = json.readTree(body).path("items");
            if (!items.isArray()) throw new IllegalArgumentException("Ollama response does not contain items");
            List<ParsedItem> result = new ArrayList<>();
            for (JsonNode item : items) {
                String name = item.path("name").asText("").trim();
                if (!name.isEmpty()) result.add(new ParsedItem(name,
                        item.hasNonNull("quantity") ? item.get("quantity").decimalValue() : null,
                        item.hasNonNull("unit") ? item.get("unit").asText() : null));
            }
            return List.copyOf(result);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse pantry text with Ollama", exception);
        }
    }
    public record ParsedItem(String name, BigDecimal quantity, String unit) { }
}
