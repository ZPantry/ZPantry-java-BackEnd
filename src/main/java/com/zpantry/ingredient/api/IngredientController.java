package com.zpantry.ingredient.api;

import static com.zpantry.ingredient.api.IngredientDtos.*;

import com.zpantry.common.api.*;
import com.zpantry.ingredient.service.IngredientService;
import com.zpantry.ingredient.service.IngredientAliasService;
import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.web.bind.annotation.*;

@RestController
public class IngredientController {
    private final IngredientService s;
    private final IngredientAliasService aliases;

    public IngredientController(IngredientService s, IngredientAliasService aliases) {
        this.s = s;
        this.aliases = aliases;
    }

    @GetMapping("/api/ingredients")
    public PagedResponse<IngredientResponse> list(@RequestParam(defaultValue = "1") int pageIndex, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String search) {
        return s.list(pageIndex, pageSize, search);
    }

    @PostMapping("/api/ingredients")
    public ApiResponse<IngredientResponse> create(@RequestBody CreateIngredientRequest r) {
        return s.create(r);
    }

    @PostMapping(value = "/api/v2/ingredients", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<IngredientResponse> createV2(@ModelAttribute IngredientFormRequest r) {
        return s.createForm(r);
    }

    @PutMapping("/api/ingredients/{id}")
    public ApiResponse<IngredientResponse> update(@PathVariable UUID id, @RequestBody UpdateIngredientRequest r) {
        return s.update(id, r);
    }

    @PutMapping(value = "/api/v2/ingredients/{id}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<IngredientResponse> updateV2(@PathVariable UUID id, @ModelAttribute IngredientFormRequest r) {
        return s.updateForm(id, r);
    }

    @DeleteMapping("/api/ingredients/{id}")
    public ApiResponse<Object> delete(@PathVariable UUID id) {
        return s.delete(id);
    }

    @GetMapping("/api/ingredients/{id}/aliases")
    public ApiResponse<java.util.List<IngredientAliasResponse>> aliases(@PathVariable UUID id) {
        return aliases.list(id);
    }

    @PostMapping("/api/ingredients/{id}/aliases")
    public ApiResponse<IngredientAliasResponse> createAlias(@PathVariable UUID id,
            @Valid @RequestBody CreateIngredientAliasRequest request) {
        return aliases.create(id, request);
    }

    @DeleteMapping("/api/ingredients/{id}/aliases/{aliasId}")
    public ApiResponse<Object> deleteAlias(@PathVariable UUID id, @PathVariable UUID aliasId) {
        return aliases.delete(id, aliasId);
    }
}
