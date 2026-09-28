package com.zpantry.integration.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpAiClient implements AiClient {
    private static final Logger log = LoggerFactory.getLogger(HttpAiClient.class);
    private final RestClient client;

    public HttpAiClient(@Value("${zpantry.ai.service-url:http://localhost:8000}") String url) {
        this.client = RestClient.builder().baseUrl(url.endsWith("/") ? url.substring(0, url.length() - 1) : url).build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> post(String path, Object request) {
        try {
            return client.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(request).retrieve().body(Map.class);
        } catch (RestClientException exception) {
            throw new AiIntegrationException("AI service request failed", exception);
        }
    }

    public Optional<float[]> embedIngredient(UUID id, String name, String normalizedName, String category) {
        try {
            return embedding(post("/ai/embed-ingredient", Map.of("ingredientId", id, "name", name,
                    "normalizedName", normalizedName, "category", category == null ? "" : category)));
        } catch (AiIntegrationException exception) {
            log.warn("Failed to embed ingredient {}: {}", id, exception.getMessage());
            return Optional.empty();
        }
    }

    public Optional<float[]> embedRecipe(UUID id, String name, String description, List<String> ingredients, String instructionText) {
        try {
            return embedding(post("/ai/embed-recipe", Map.of("recipeId", id, "name", name,
                    "description", description == null ? "" : description, "ingredientNames", ingredients,
                    "instructionText", instructionText == null ? "" : instructionText)));
        } catch (AiIntegrationException exception) {
            log.warn("Failed to embed recipe {}: {}", id, exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<float[]> embedding(Map<String, Object> response) {
        Object data = response == null ? null : response.get("data");
        if (data instanceof Map<?, ?> dataMap && dataMap.get("embedding") instanceof List<?> values) {
            float[] vector = new float[values.size()];
            for (int index = 0; index < values.size(); index++) {
                vector[index] = ((Number) values.get(index)).floatValue();
            }
            return Optional.of(vector);
        }
        return Optional.empty();
    }

    public static class AiIntegrationException extends RuntimeException {
        public AiIntegrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
