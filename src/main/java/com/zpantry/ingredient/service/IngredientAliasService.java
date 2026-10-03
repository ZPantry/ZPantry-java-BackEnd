package com.zpantry.ingredient.service;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.ingredient.api.IngredientDtos.CreateIngredientAliasRequest;
import com.zpantry.ingredient.api.IngredientDtos.IngredientAliasResponse;
import com.zpantry.ingredient.domain.IngredientAliasEntity;
import com.zpantry.ingredient.persistence.IngredientAliasRepository;
import com.zpantry.ingredient.persistence.IngredientRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngredientAliasService {
    private final IngredientRepository ingredients;
    private final IngredientAliasRepository aliases;

    public IngredientAliasService(IngredientRepository ingredients, IngredientAliasRepository aliases) {
        this.ingredients = ingredients;
        this.aliases = aliases;
    }

    public ApiResponse<List<IngredientAliasResponse>> list(UUID ingredientId) {
        if (ingredients.findByIdAndDeletedFalse(ingredientId).isEmpty()) return fail("Ingredient not found.");
        return ok(aliases.findAllByIngredientIdAndDeletedFalse(ingredientId).stream().map(this::dto).toList(), "");
    }

    @Transactional
    public ApiResponse<IngredientAliasResponse> create(UUID ingredientId, CreateIngredientAliasRequest request) {
        if (ingredients.findByIdAndDeletedFalse(ingredientId).isEmpty()) return fail("Ingredient not found.");
        String aliasName = request.aliasName().trim();
        String normalized = FoodNameNormalizer.normalize(aliasName);
        if (ingredients.findByNormalizedNameAndDeletedFalse(normalized).isPresent()
                || aliases.existsByNormalizedAliasNameAndDeletedFalse(normalized)) {
            return fail("Food alias already exists or conflicts with a canonical food.");
        }
        IngredientAliasEntity saved = aliases.save(new IngredientAliasEntity(ingredientId, aliasName, normalized));
        return ok(dto(saved), "Food alias created.");
    }

    @Transactional
    public ApiResponse<Object> delete(UUID ingredientId, UUID aliasId) {
        var alias = aliases.findById(aliasId).filter(item -> !item.isDeleted() && ingredientId.equals(item.ingredientId)).orElse(null);
        if (alias == null) return fail("Food alias not found.");
        alias.softDelete();
        return ok(null, "Food alias deleted.");
    }

    private IngredientAliasResponse dto(IngredientAliasEntity entity) {
        return new IngredientAliasResponse(entity.getId(), entity.ingredientId, entity.aliasName, entity.normalizedAliasName);
    }

    private static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, null, "", Instant.now());
    }

    private static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null, null, "", Instant.now());
    }
}
