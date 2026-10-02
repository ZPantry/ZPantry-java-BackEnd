package com.zpantry.integration.ai;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Component
public class HttpAiClient implements AiClient {
    private static final Logger log = LoggerFactory.getLogger(HttpAiClient.class);
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public HttpAiClient(@Value("${zpantry.ai.service-url:http://localhost:8000}") String url, ObjectMapper objectMapper) {
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> post(String path, Object request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            log.info("Calling AI endpoint {} with JSON payload size {} bytes", path, json.length());
            byte[] payload = json.getBytes(StandardCharsets.UTF_8);
            var connection = (HttpURLConnection) URI.create(baseUrl + path).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            connection.setFixedLengthStreamingMode(payload.length);
            try (var output = connection.getOutputStream()) {
                output.write(payload);
            }
            int status = connection.getResponseCode();
            InputStream responseBody = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            String response = responseBody == null ? "" : new String(responseBody.readAllBytes(), StandardCharsets.UTF_8);
            if (status < 200 || status >= 300) {
                throw new AiIntegrationException("AI service responded with HTTP " + status, null);
            }
            return objectMapper.readValue(response, Map.class);
        } catch (JacksonException | IOException exception) {
            throw new AiIntegrationException("AI service request failed", exception);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> postImage(String path, byte[] image, String filename, String contentType) {
        try {
            String boundary = "----ZPantry" + UUID.randomUUID();
            String safeFilename = filename == null ? "image" : filename.replace("\"", "");
            String mimeType = contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bytes.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            bytes.write(("Content-Disposition: form-data; name=\"image\"; filename=\"" + safeFilename + "\"\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            bytes.write(("Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            bytes.write(image);
            bytes.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            byte[] payload = bytes.toByteArray();
            var connection = (HttpURLConnection) URI.create(baseUrl + path).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE + "; boundary=" + boundary);
            connection.setFixedLengthStreamingMode(payload.length);
            try (var output = connection.getOutputStream()) {
                output.write(payload);
            }
            int status = connection.getResponseCode();
            InputStream responseBody = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            String response = responseBody == null ? "" : new String(responseBody.readAllBytes(), StandardCharsets.UTF_8);
            if (status < 200 || status >= 300) throw new AiIntegrationException("AI service responded with HTTP " + status, null);
            return objectMapper.readValue(response, Map.class);
        } catch (JacksonException | IOException exception) {
            throw new AiIntegrationException("AI image analysis failed", exception);
        }
    }

    public Optional<float[]> embedIngredient(UUID id, String name, String normalizedName, String category) {
        try {
            Map<String, Object> request = new java.util.HashMap<>();
            request.put("ingredientId", id);
            request.put("name", name);
            request.put("normalizedName", normalizedName);
            request.put("category", category == null ? "" : category);
            return embedding(post("/ai/embed-ingredient", request));
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
