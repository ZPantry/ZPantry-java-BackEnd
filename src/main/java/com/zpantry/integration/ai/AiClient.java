package com.zpantry.integration.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AiClient {
    Optional<float[]> embedIngredient(UUID id, String name, String normalized, String category);

    Optional<float[]> embedRecipe(UUID id, String name, String description, List<String> ingredients, String instructions);

    Map<String, Object> post(String path, Object request);

    default Map<String, Object> postImage(String path, byte[] image, String filename, String contentType) {
        throw new UnsupportedOperationException("Image analysis is unavailable");
    }
}
