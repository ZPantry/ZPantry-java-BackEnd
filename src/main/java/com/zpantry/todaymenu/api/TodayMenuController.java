package com.zpantry.todaymenu.api;

import static com.zpantry.todaymenu.api.TodayMenuDtos.*;

import com.zpantry.common.api.*;
import com.zpantry.todaymenu.service.TodayMenuService;
import com.zpantry.user.security.AuthenticatedUserResolver;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

@RestController
public class TodayMenuController {
    private final TodayMenuService s;
    private final AuthenticatedUserResolver ids;
    private final com.zpantry.todaymenu.service.DailyNutritionService nutrition;

    public TodayMenuController(TodayMenuService s, AuthenticatedUserResolver i, com.zpantry.todaymenu.service.DailyNutritionService nutrition) {
        this.s = s;
        ids = i;
        this.nutrition = nutrition;
    }

    private UUID id(Authentication a) {
        return ids.resolve(a).orElseThrow().userId();
    }

    @GetMapping("/api/me/today-menu")
    public PagedResponse<TodayMenuItemResponse> list(Authentication a, @RequestParam(required = false) LocalDate date, @RequestParam(defaultValue = "1") int pageIndex, @RequestParam(defaultValue = "10") int pageSize) {
        return s.list(id(a), date, pageIndex, pageSize);
    }

    @GetMapping("/api/me/today-menu/items/{itemId}")
    public ApiResponse<TodayMenuItemResponse> get(Authentication a, @PathVariable UUID itemId) {
        return s.get(id(a), itemId);
    }

    @GetMapping("/api/me/today-menu/items/{itemId}/ingredient-availability")
    public ApiResponse<IngredientAvailabilityResponse> ingredientAvailability(Authentication a, @PathVariable UUID itemId) {
        return s.ingredientAvailability(id(a), itemId);
    }

    @PostMapping("/api/me/today-menu/items/{itemId}/missing-ingredients")
    public ApiResponse<List<ShoppingListItemResponse>> addMissingIngredients(Authentication a, @PathVariable UUID itemId) {
        return s.addMissingIngredientsToShoppingList(id(a), itemId);
    }

    @PostMapping("/api/me/today-menu/items")
    public ApiResponse<TodayMenuItemResponse> create(Authentication a, @RequestBody CreateTodayMenuItemRequest r) {
        return s.create(id(a), r);
    }

    @DeleteMapping("/api/me/today-menu/items/{itemId}")
    public ApiResponse<Object> delete(Authentication a, @PathVariable UUID itemId) {
        return s.delete(id(a), itemId);
    }

    @PostMapping(path = "/api/me/today-menu/items/{itemId}/complete", consumes = "multipart/form-data")
    public ApiResponse<TodayMenuCompletionResponse> complete(Authentication a, @PathVariable UUID itemId,
                                                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant cookedAt,
                                                               @RequestParam(required = false) Integer rating,
                                                               @RequestParam(required = false) String note) {
        return s.complete(id(a), itemId, new CompleteTodayMenuItemRequest(imageFile, cookedAt, rating, note));
    }

    @GetMapping("/api/me/cooking-logs")
    public PagedResponse<CookingLogResponse> logs(Authentication a, @RequestParam(defaultValue = "1") int pageIndex, @RequestParam(defaultValue = "10") int pageSize) {
        return s.logs(id(a), pageIndex, pageSize);
    }
    @GetMapping("/api/me/nutrition/daily")
    public ApiResponse<DailyNutritionResponse> nutrition(Authentication a, @RequestParam(required=false) LocalDate date) { return new ApiResponse<>(true,"",nutrition.daily(id(a),date),null,"",java.time.Instant.now()); }
}
