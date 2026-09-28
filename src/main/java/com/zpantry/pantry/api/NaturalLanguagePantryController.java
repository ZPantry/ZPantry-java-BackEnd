package com.zpantry.pantry.api;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.ingredient.persistence.IngredientRepository;
import com.zpantry.pantry.service.OllamaPantryParser;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/pantry")
public class NaturalLanguagePantryController {
    private final OllamaPantryParser parser;
    private final IngredientRepository ingredients;
    public NaturalLanguagePantryController(OllamaPantryParser parser, IngredientRepository ingredients) {
        this.parser = parser; this.ingredients = ingredients;
    }
    @PostMapping("/parse")
    public ApiResponse<List<PreviewItem>> parse(@RequestBody ParseRequest request) {
        var preview = parser.parse(request.text()).stream().map(item -> {
            var candidates = ingredients.searchActive(item.name().toLowerCase(), PageRequest.of(0, 1)).getContent();
            var match = candidates.isEmpty() ? null : candidates.getFirst();
            return new PreviewItem(item.name(), item.quantity(), item.unit(), match == null ? null : match.getId(),
                    match == null ? null : match.name, match != null);
        }).toList();
        return new ApiResponse<>(true, "Preview only; confirm before saving.", preview, null, "", Instant.now());
    }
    public record ParseRequest(String text) { }
    public record PreviewItem(String name, java.math.BigDecimal quantity, String unit,
            java.util.UUID ingredientId, String ingredientName, boolean matched) { }
}
