package com.zpantry.pantry.service;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.common.api.PagedResponse;
import com.zpantry.ingredient.persistence.IngredientRepository;
import com.zpantry.pantry.domain.PantryItemEntity;
import com.zpantry.pantry.persistence.PantryItemRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.zpantry.pantry.api.PantryDtos.*;

@Service
public class PantryService {
    private final PantryItemRepository repo;
    private final IngredientRepository ingredients;

    public PantryService(PantryItemRepository r, IngredientRepository i) {
        repo = r;
        ingredients = i;
    }

    private static <T> ApiResponse<T> ok(T d, String m) {
        return new ApiResponse<>(true, m, d, null, "", Instant.now());
    }

    private static <T> ApiResponse<T> fail(String m) {
        return new ApiResponse<>(false, m, null, null, "", Instant.now());
    }

    public PagedResponse<PantryItemResponse> list(UUID u, int pi, int ps) {
        pi = Math.max(1, pi);
        ps = ps <= 0 ? 10 : Math.min(ps, 100);
        var p = repo.findAllByUserIdAndDeletedFalse(u, PageRequest.of(pi - 1, ps, Sort.by("expiredAt").ascending().and(Sort.by("createdAt"))));
        return PagedResponse.successPage(p.stream().map(this::dto).toList(), pi, ps, (int) p.getTotalElements(), "", "", Instant.now());
    }

    @Transactional
    public ApiResponse<PantryItemResponse> upsert(UUID u, UpsertPantryItemRequest r) {
        String invalid = invalidItem(r.ingredientId(), r.quantity(), r.unit());
        if (invalid != null) return fail(invalid);
        var e = repo.findByUserIdAndIngredientIdAndDeletedFalse(u, r.ingredientId()).orElseGet(() -> new PantryItemEntity(u, r.ingredientId()));
        apply(e, r.ingredientId(), r.quantity(), r.unit(), r.expiredAt(), r.storageLocation(), r.note());
        return ok(dto(repo.save(e)), "Pantry item saved.");
    }

    @Transactional
    public ApiResponse<List<PantryItemResponse>> upsertBatch(UUID userId, List<UpsertPantryItemRequest> requests) {
        if (requests == null || requests.isEmpty()) return fail("At least one pantry item is required.");
        Set<UUID> ingredientIds = new HashSet<>();
        for (UpsertPantryItemRequest request : requests) {
            String invalid = invalidItem(request.ingredientId(), request.quantity(), request.unit());
            if (invalid != null) return fail(invalid);
            if (!ingredientIds.add(request.ingredientId())) return fail("Each ingredient can appear only once.");
        }

        var saved = requests.stream().map(request -> {
            var item = repo.findByUserIdAndIngredientIdAndDeletedFalse(userId, request.ingredientId())
                    .orElseGet(() -> new PantryItemEntity(userId, request.ingredientId()));
            apply(item, request.ingredientId(), request.quantity(), request.unit(), request.expiredAt(),
                    request.storageLocation(), request.note());
            return dto(repo.save(item));
        }).toList();
        return ok(saved, "Pantry items saved.");
    }

    @Transactional
    public ApiResponse<PantryItemResponse> update(UUID u, UUID id, UpdatePantryItemRequest r) {
        return update(u, id, r, true);
    }

    @Transactional
    public ApiResponse<PantryItemResponse> update(UUID u, UUID id, UpdatePantryItemRequest r, boolean expiredAtProvided) {
        var e = repo.findByIdAndUserIdAndDeletedFalse(id, u).orElse(null);
        if (e == null) return fail("Pantry item not found.");
        UUID ingredientId = r.ingredientId() == null ? e.ingredientId : r.ingredientId();
        String invalid = invalidItem(ingredientId, r.quantity() == null ? e.quantity : r.quantity(), r.unit() == null ? e.unit : r.unit());
        if (invalid != null) return fail(invalid);
        apply(e, r.ingredientId(), r.quantity(), r.unit(), r.expiredAt(), expiredAtProvided, r.storageLocation(), r.note());
        e.touch();
        return ok(dto(e), "Pantry item updated.");
    }

    @Transactional
    public ApiResponse<Object> delete(UUID u, UUID id) {
        var e = repo.findByIdAndUserIdAndDeletedFalse(id, u).orElse(null);
        if (e == null) return fail("Pantry item not found.");
        e.softDelete();
        return ok(null, "Pantry item deleted.");
    }

    public void validateImportItem(UUID ingredientId, java.math.BigDecimal quantity, String unit) {
        String invalid = invalidItem(ingredientId, quantity, unit);
        if (invalid != null) throw new IllegalArgumentException(invalid);
    }

    private void apply(PantryItemEntity e, UUID i, java.math.BigDecimal q, String unit, Instant ex, String loc, String note) {
        apply(e, i, q, unit, ex, false, loc, note);
    }

    private void apply(PantryItemEntity e, UUID i, java.math.BigDecimal q, String unit, Instant ex, boolean expiredAtProvided, String loc, String note) {
        if (i != null) e.ingredientId = i;
        if (q != null) e.quantity = q;
        if (unit != null) e.unit = unit;
        if (expiredAtProvided || ex != null) e.expiredAt = ex;
        if (loc != null) e.storageLocation = loc;
        if (note != null) e.note = note;
    }

    private String invalidItem(UUID ingredientId, java.math.BigDecimal quantity, String unit) {
        if (ingredientId == null || ingredients.findByIdAndDeletedFalse(ingredientId).isEmpty()) return "Ingredient not found.";
        if (quantity == null || quantity.signum() <= 0 || unit == null || unit.isBlank() || unit.length() > 50) return "Invalid pantry item.";
        return null;
    }

    private PantryItemResponse dto(PantryItemEntity e) {
        return new PantryItemResponse(e.getId(), e.ingredientId, ingredients.findById(e.ingredientId).map(x -> x.name).orElse(null), e.quantity, e.unit, e.expiredAt, e.storageLocation, e.note);
    }
}
