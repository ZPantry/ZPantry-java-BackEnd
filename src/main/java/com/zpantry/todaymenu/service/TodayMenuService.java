package com.zpantry.todaymenu.service;

import static com.zpantry.todaymenu.api.TodayMenuDtos.*;

import com.zpantry.common.api.*;
import com.zpantry.ingredient.persistence.IngredientRepository;
import com.zpantry.media.service.MediaStoragePort;
import com.zpantry.pantry.persistence.PantryItemRepository;
import com.zpantry.recipe.persistence.*;
import com.zpantry.todaymenu.domain.*;
import com.zpantry.todaymenu.persistence.*;

import java.time.*;
import java.util.*;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TodayMenuService {
    private final TodayMenuItemRepository menus;
    private final CookingLogRepository logs;
    private final PantryUsageLogRepository usage;
    private final MediaStoragePort media;
    private final RecipeRepository recipes;
    private final RecipeIngredientRepository recipeIngredients;
    private final PantryItemRepository pantry;
    private final IngredientRepository ingredients;
    private final ShoppingListItemRepository shoppingList;

    public TodayMenuService(TodayMenuItemRepository m, CookingLogRepository l, PantryUsageLogRepository u, MediaStoragePort s, RecipeRepository recipes, RecipeIngredientRepository recipeIngredients, PantryItemRepository pantry, IngredientRepository ingredients, ShoppingListItemRepository shoppingList) {
        menus = m;
        logs = l;
        usage = u;
        media = s;
        this.recipes = recipes;
        this.recipeIngredients = recipeIngredients;
        this.pantry = pantry;
        this.ingredients = ingredients;
        this.shoppingList = shoppingList;
    }

    private static <T> ApiResponse<T> ok(T d, String m) {
        return new ApiResponse<>(true, m, d, null, "", Instant.now());
    }

    private static <T> ApiResponse<T> fail(String m) {
        return new ApiResponse<>(false, m, null, null, "", Instant.now());
    }

    public PagedResponse<TodayMenuItemResponse> list(UUID u, LocalDate d, int pi, int ps) {
        pi = Math.max(1, pi);
        ps = ps <= 0 ? 10 : Math.min(ps, 100);
        var p = menus.findAllByUserIdAndPlannedDateAndDeletedFalse(u, d == null ? LocalDate.now() : d, PageRequest.of(pi - 1, ps, Sort.by("createdAt")));
        return PagedResponse.successPage(p.stream().map(this::dto).toList(), pi, ps, (int) p.getTotalElements(), "", "", Instant.now());
    }

    public ApiResponse<TodayMenuItemResponse> get(UUID u, UUID id) {
        var e = menus.findByIdAndUserIdAndDeletedFalse(id, u).orElse(null);
        return e == null ? fail("Today menu item not found.") : ok(dto(e), "");
    }

    public ApiResponse<IngredientAvailabilityResponse> ingredientAvailability(UUID userId, UUID itemId) {
        var item = menus.findByIdAndUserIdAndDeletedFalse(itemId, userId).orElse(null);
        if (item == null) return fail("Today menu item not found.");
        var recipeId = resolvedRecipeId(item);
        if (recipeId == null || recipes.findByIdAndDeletedFalse(recipeId).isEmpty()) return fail("Recipe not found.");
        return ok(availability(userId, item, recipeId), "");
    }

    @Transactional
    public ApiResponse<List<ShoppingListItemResponse>> addMissingIngredientsToShoppingList(UUID userId, UUID itemId) {
        var item = menus.findByIdAndUserIdAndDeletedFalse(itemId, userId).orElse(null);
        if (item == null) return fail("Today menu item not found.");
        var recipeId = resolvedRecipeId(item);
        if (recipeId == null || recipes.findByIdAndDeletedFalse(recipeId).isEmpty()) return fail("Recipe not found.");
        var missing = availability(userId, item, recipeId).ingredients().stream()
                .filter(line -> line.missingQuantity().signum() > 0).toList();
        var saved = missing.stream().map(line -> {
            var entity = shoppingList.findByUserIdAndTodayMenuItemIdAndIngredientIdAndUnitAndDeletedFalse(
                    userId, item.getId(), line.ingredientId(), line.unit()).orElseGet(ShoppingListItemEntity::new);
            entity.userId = userId;
            entity.todayMenuItemId = item.getId();
            entity.ingredientId = line.ingredientId();
            entity.ingredientName = line.ingredientName();
            entity.quantity = line.missingQuantity();
            entity.unit = line.unit();
            entity.status = "PENDING";
            entity.touch();
            return shoppingDto(shoppingList.save(entity));
        }).toList();
        return ok(saved, saved.isEmpty() ? "Pantry already has all required ingredients." : "Missing ingredients added to shopping list.");
    }

    @Transactional
    public ApiResponse<TodayMenuItemResponse> create(UUID u, CreateTodayMenuItemRequest r) {
        if (r.mealName() == null || r.mealName().isBlank()) return fail("Meal name is required.");
        var e = new TodayMenuItemEntity(u);
        e.mealId = r.mealId();
        e.recipeId = r.recipeId();
        e.mealName = r.mealName();
        e.mealType = r.mealType();
        e.servingSize = r.servingSize();
        e.plannedDate = r.plannedDate() == null ? LocalDate.now() : r.plannedDate();
        e.note = r.note();
        return ok(dto(menus.save(e)), "Meal added to today menu.");
    }

    @Transactional
    public ApiResponse<Object> delete(UUID u, UUID id) {
        var e = menus.findByIdAndUserIdAndDeletedFalse(id, u).orElse(null);
        if (e == null) return fail("Today menu item not found.");
        if ("Cooked".equals(e.status)) return fail("Cooked menu items cannot be deleted.");
        e.status = "Cancelled";
        e.softDelete();
        return ok(null, "Today menu item deleted.");
    }

    @Transactional
    public ApiResponse<TodayMenuCompletionResponse> complete(UUID u, UUID id, CompleteTodayMenuItemRequest r) {
        if (r.imageFile() == null || r.imageFile().isEmpty()) return fail("ImageFile is required.");
        var e = menus.findByIdAndUserIdAndDeletedFalse(id, u).orElse(null);
        if (e == null) return fail("Today menu item not found.");
        if ("Cooked".equals(e.status)) return fail("This meal has already been completed.");
        UUID recipeId = resolvedRecipeId(e);
        if (recipeId == null) return fail("This today menu item does not have a resolved recipe.");
        if (recipes.findByIdAndDeletedFalse(recipeId).isEmpty()) return fail("Recipe not found.");
        var up = media.upload(r.imageFile(), "cooking");
        e.status = "Cooked";
        e.cookedAt = r.cookedAt() == null ? Instant.now() : r.cookedAt();
        e.imageUrl = up.secureUrl();
        e.imagePublicId = up.publicId();
        e.touch();
        var l = new CookingLogEntity(u, e);
        l.imageUrl = e.imageUrl;
        l.imagePublicId = e.imagePublicId;
        l.rating = r.rating();
        l.note = r.note() == null ? e.note : r.note();
        l = logs.save(l);
        List<Object> consumed = new ArrayList<>(), updated = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        var pantryRows = pantry.findAllByUserIdAndDeletedFalseOrderByExpiredAtAscCreatedAtAsc(u);
        var recipe = recipes.findByIdAndDeletedFalse(recipeId).orElseThrow();
        for (var needed : recipeIngredients.findAllByRecipeIdAndDeletedFalse(recipeId)) {
            if (!needed.required) continue;
            String name = ingredients.findById(needed.ingredientId).map(x -> x.name).orElse(needed.ingredientId.toString());
            if (needed.quantity == null || needed.quantity.signum() <= 0) {
                warnings.add("Ingredient " + name + " does not have a usable quantity.");
                continue;
            }
            var remaining = scaledQuantity(needed.quantity, e.servingSize, recipe.servingSize);
            for (var item : pantryRows) {
                if (remaining.signum() <= 0) break;
                if (!item.ingredientId.equals(needed.ingredientId)) continue;
                if (item.unit != null && needed.unit != null && !item.unit.equalsIgnoreCase(needed.unit)) {
                    warnings.add("Unit mismatch for " + name + ": pantry='" + item.unit + "', recipe='" + needed.unit + "'.");
                    continue;
                }
                if (item.quantity == null) {
                    warnings.add("Pantry quantity is missing for " + name + ".");
                    continue;
                }
                var amount = item.quantity.min(remaining);
                item.quantity = item.quantity.subtract(amount);
                item.touch();
                if (item.quantity.signum() <= 0) item.softDelete();
                remaining = remaining.subtract(amount);
                var ul = new PantryUsageLogEntity();
                ul.userId = u;
                ul.todayMenuItemId = e.getId();
                ul.cookingLogId = l.getId();
                ul.ingredientId = needed.ingredientId;
                ul.ingredientName = name;
                ul.quantityUsed = amount;
                ul.unit = item.unit == null ? needed.unit : item.unit;
                usage.save(ul);
                consumed.add(Map.of("ingredientId", needed.ingredientId, "ingredientName", name, "quantityUsed", amount, "unit", ul.unit == null ? "" : ul.unit, "actionType", "consumed"));
                updated.add(Map.of("id", item.getId(), "ingredientId", item.ingredientId, "quantity", item.quantity));
            }
            if (remaining.signum() > 0) warnings.add("Not enough pantry quantity for " + name + ".");
        }
        var lr = new CookingLogResponse(l.getId(), l.todayMenuItemId, l.mealId, l.recipeId, l.mealName, l.imageUrl, l.imagePublicId, l.cookedAt, l.rating, l.note, consumed);
        return ok(new TodayMenuCompletionResponse(lr, consumed, updated, warnings), "Meal completed and cooking log saved.");
    }

    public PagedResponse<CookingLogResponse> logs(UUID u, int pi, int ps) {
        pi = Math.max(1, pi);
        ps = ps <= 0 ? 10 : Math.min(ps, 100);
        var p = logs.findAllByUserIdAndDeletedFalse(u, PageRequest.of(pi - 1, ps, Sort.by("cookedAt").descending()));
        var d = p.stream().map(l -> new CookingLogResponse(l.getId(), l.todayMenuItemId, l.mealId, l.recipeId, l.mealName, l.imageUrl, l.imagePublicId, l.cookedAt, l.rating, l.note, usage.findAllByCookingLogIdAndDeletedFalseOrderByCreatedAtAsc(l.getId()).stream().<Object>map(this::usageDto).toList())).toList();
        return PagedResponse.successPage(d, pi, ps, (int) p.getTotalElements(), "", "", Instant.now());
    }

    private TodayMenuItemResponse dto(TodayMenuItemEntity e) {
        return new TodayMenuItemResponse(e.getId(), e.mealId, e.recipeId, e.mealName, e.mealType, e.servingSize, e.plannedDate, e.status, e.note, e.cookedAt, e.imageUrl, e.imagePublicId, e.getCreatedAt());
    }

    private UUID resolvedRecipeId(TodayMenuItemEntity item) {
        return item.recipeId != null ? item.recipeId : item.mealId;
    }

    private IngredientAvailabilityResponse availability(UUID userId, TodayMenuItemEntity item, UUID recipeId) {
        var recipe = recipes.findByIdAndDeletedFalse(recipeId).orElseThrow();
        var pantryRows = pantry.findAllByUserIdAndDeletedFalseOrderByExpiredAtAscCreatedAtAsc(userId);
        var lines = new ArrayList<IngredientAvailability>();
        for (var needed : recipeIngredients.findAllByRecipeIdAndDeletedFalse(recipeId)) {
            if (!needed.required || needed.quantity == null || needed.quantity.signum() <= 0) continue;
            var required = scaledQuantity(needed.quantity, item.servingSize, recipe.servingSize);
            var matching = pantryRows.stream().filter(row -> row.ingredientId.equals(needed.ingredientId)).toList();
            var compatible = matching.stream().filter(row -> unitsMatch(row.unit, needed.unit))
                    .map(row -> row.quantity == null ? java.math.BigDecimal.ZERO : row.quantity)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            var missing = required.subtract(compatible).max(java.math.BigDecimal.ZERO);
            var name = ingredients.findById(needed.ingredientId).map(x -> x.name).orElse(needed.ingredientId.toString());
            var unitMismatch = !matching.isEmpty() && compatible.signum() == 0;
            lines.add(new IngredientAvailability(needed.ingredientId, name, required, compatible, missing,
                    needed.unit == null ? "" : needed.unit, unitMismatch));
        }
        return new IngredientAvailabilityResponse(item.getId(), lines.stream().allMatch(line -> line.missingQuantity().signum() == 0), lines);
    }

    private java.math.BigDecimal scaledQuantity(java.math.BigDecimal quantity, Integer selectedServings, Integer recipeServings) {
        if (selectedServings == null || selectedServings <= 0 || recipeServings == null || recipeServings <= 0) return quantity;
        return quantity.multiply(java.math.BigDecimal.valueOf(selectedServings))
                .divide(java.math.BigDecimal.valueOf(recipeServings), 4, java.math.RoundingMode.HALF_UP);
    }

    private boolean unitsMatch(String pantryUnit, String recipeUnit) {
        return pantryUnit == null || recipeUnit == null || pantryUnit.equalsIgnoreCase(recipeUnit);
    }

    private Map<String, Object> usageDto(PantryUsageLogEntity row) {
        return Map.of("id", row.getId(), "ingredientId", row.ingredientId, "ingredientName", row.ingredientName,
                "quantityUsed", row.quantityUsed, "unit", row.unit == null ? "" : row.unit,
                "actionType", row.actionType, "warning", row.warning == null ? "" : row.warning);
    }

    private ShoppingListItemResponse shoppingDto(ShoppingListItemEntity item) {
        return new ShoppingListItemResponse(item.getId(), item.todayMenuItemId, item.ingredientId,
                item.ingredientName, item.quantity, item.unit, item.status, item.getCreatedAt());
    }
}
