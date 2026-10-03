package com.zpantry.pantry.api;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.pantryimport.api.PantryImportDtos.PantryImportPreviewItem;
import com.zpantry.pantry.service.PantryTextExtractionService;
import com.zpantry.pantryimport.service.PantryIngredientPipeline;
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
    private final PantryTextExtractionService extraction;
    private final PantryIngredientPipeline pipeline;

    public NaturalLanguagePantryController(PantryTextExtractionService extraction, PantryIngredientPipeline pipeline) {
        this.extraction = extraction;
        this.pipeline = pipeline;
    }
    @PostMapping("/parse")
    public ApiResponse<List<PantryImportPreviewItem>> parse(@Valid @RequestBody ParseRequest request) {
        var resolved = new LinkedHashMap<java.util.UUID, PantryImportPreviewItem>();
        for (var item : extraction.extract(request.text())) {
            pipeline.resolve(item).ifPresent(preview -> resolved.putIfAbsent(preview.ingredientId(), preview));
        }
        var preview = List.copyOf(resolved.values());
        return new ApiResponse<>(true, "Preview only; confirm before saving.", preview, null, "", Instant.now());
    }
    public record ParseRequest(@NotBlank @Size(max = 2_000) String text) { }
}
