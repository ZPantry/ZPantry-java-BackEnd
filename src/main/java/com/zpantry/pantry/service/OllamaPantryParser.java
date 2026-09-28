package com.zpantry.pantry.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
            String prompt = "You are an ingredient extraction component for ZPantry. Extract only food ingredients explicitly mentioned in the input. Never invent, infer, recommend, answer questions, or infer ingredients from dishes. If no food ingredient is explicitly present, return {\\\"isRelevant\\\":false,\\\"reason\\\":\\\"No ingredient information found.\\\",\\\"ingredients\\\":[]}. Otherwise return {\\\"isRelevant\\\":true,\\\"reason\\\":null,\\\"ingredients\\\":[{\\\"name\\\":\\\"string without a leading number\\\",\\\"quantity\\\":number|null,\\\"unit\\\":\\\"string|null\\\"}]}. Quantity and unit must be null when not explicitly stated. Return JSON only. Input: " + text;
            Map<?, ?> reply = client.post().uri("/api/generate")
                    .body(Map.of("model", model, "prompt", prompt, "stream", false, "format", "json"))
                    .retrieve().body(Map.class);
            Object response = reply == null ? null : reply.get("response");
            if (!(response instanceof String body))
                throw new IllegalArgumentException("Ollama returned no JSON response");
            JsonNode items = json.readTree(body).path("ingredients");
            if (!items.isArray()) throw new IllegalArgumentException("Ollama response does not contain items");
            List<ParsedItem> result = new ArrayList<>();
            for (JsonNode item : items) {
                String name = item.path("name").asText("").replaceFirst("^\\s*\\d+(?:[.,]\\d+)?\\s*", "").trim();
                if (!name.isEmpty()) {
                    BigDecimal quantity = item.hasNonNull("quantity") ? item.get("quantity").decimalValue() : null;
                    String parsedUnit = item.hasNonNull("unit") ? item.get("unit").asText().trim() : null;
                    result.add(new ParsedItem(name, quantity, normalizeUnit(name, parsedUnit)));
                }
            }
            return List.copyOf(result);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse pantry text with Ollama", exception);
        }
    }

    private static String normalizeUnit(String name, String parsedUnit) {
        String normalized = name.toLowerCase(Locale.ROOT);
        String canonical = containsAny(normalized, "thịt", "cá", "tôm", "mực", "cua", "nghêu", "sò") ? "g"
                : containsAny(normalized, "sữa chua", "trứng", "cà chua", "dưa leo", "hành tây") ? "cái"
                : containsAny(normalized, "sữa", "nước mắm", "dầu", "nước tương") ? "ml"
                : containsAny(normalized, "rau muống", "rau cải", "rau thơm") ? "bó" : null;
        if (parsedUnit == null || parsedUnit.isBlank()) return canonical;
        String unit = parsedUnit.toLowerCase(Locale.ROOT);
        return switch (unit) {
            case "g", "kg", "ml", "l", "cái", "bó", "miếng", "quả" -> unit;
            default -> canonical;
        };
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    public record ParsedItem(String name, BigDecimal quantity, String unit) {
    }
}
