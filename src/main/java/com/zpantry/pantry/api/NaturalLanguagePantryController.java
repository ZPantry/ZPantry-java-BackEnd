package com.zpantry.pantry.api;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.ingredient.persistence.IngredientRepository;
import com.zpantry.pantryimport.api.PantryImportDtos.PantryImportPreviewItem;
import com.zpantry.pantryimport.service.CatalogPreviewFactory;
import com.zpantry.pantry.service.OllamaPantryParser;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/pantry")
public class NaturalLanguagePantryController {
    private final OllamaPantryParser parser;
    private final CatalogPreviewFactory previews;
    private final IngredientRepository ingredients;
    public NaturalLanguagePantryController(OllamaPantryParser parser, CatalogPreviewFactory previews, IngredientRepository ingredients) {
        this.parser = parser; this.previews = previews; this.ingredients = ingredients;
    }
    @PostMapping("/parse")
    public ApiResponse<List<PantryImportPreviewItem>> parse(@Valid @RequestBody ParseRequest request) {
        var catalogNames = ingredients.findAllByDeletedFalse(org.springframework.data.domain.Pageable.unpaged()).stream()
                .map(ingredient -> ingredient.name).toList();
        var parsed = parser.parse(request.text(), catalogNames);
        var resolved = new LinkedHashMap<java.util.UUID, PantryImportPreviewItem>();
        for (var item : parsed) addResolved(resolved, item.name(), item.quantity(), request.text());
        for (String phrase : request.text().split("[,;\\n]|\\bvà\\b")) addResolved(resolved, phrase.trim(), null, request.text());
        var preview = List.copyOf(resolved.values());
        return new ApiResponse<>(true, "Preview only; confirm before saving.", preview, null, "", Instant.now());
    }
    private void addResolved(LinkedHashMap<java.util.UUID, PantryImportPreviewItem> resolved, String name,
            java.math.BigDecimal quantity, String sourceText) {
        previews.resolved(name, hasExplicitQuantity(sourceText) ? quantity : null, null, null)
                .filter(item -> isExplicitlyMentioned(sourceText, item.ingredient().normalizedName()))
                .ifPresent(item -> resolved.putIfAbsent(item.ingredientId(), item));
    }
    private static boolean hasExplicitQuantity(String text) {
        return text != null && text.matches(".*\\d+.*");
    }
    private static boolean isExplicitlyMentioned(String sourceText, String canonicalName) {
        String input = normalize(sourceText);
        String canonical = normalize(canonicalName);
        if (input.contains(canonical)) return true;
        for (String token : canonical.split(" ")) if (token.length() > 2 && input.contains(token)) return true;
        return false;
    }
    private static String normalize(String text) {
        return java.text.Normalizer.normalize(text == null ? "" : text.trim(), java.text.Normalizer.Form.NFKC)
                .toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ");
    }
    public record ParseRequest(@NotBlank @Size(max = 2_000) String text) { }
}
