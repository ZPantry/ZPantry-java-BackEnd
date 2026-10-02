package com.zpantry.ingredient.api;

import static com.zpantry.ingredient.api.IngredientDtos.*;

import com.zpantry.common.api.*;
import com.zpantry.ingredient.service.IngredientService;

import java.util.UUID;

import org.springframework.web.bind.annotation.*;

@RestController
public class IngredientController {
    private final IngredientService s;

    public IngredientController(IngredientService s) {
        this.s = s;
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
}
