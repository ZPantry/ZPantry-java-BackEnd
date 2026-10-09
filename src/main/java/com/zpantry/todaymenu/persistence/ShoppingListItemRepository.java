package com.zpantry.todaymenu.persistence;

import com.zpantry.todaymenu.domain.ShoppingListItemEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItemEntity, UUID> {
    Optional<ShoppingListItemEntity> findByUserIdAndTodayMenuItemIdAndIngredientIdAndUnitAndDeletedFalse(
            UUID userId, UUID todayMenuItemId, UUID ingredientId, String unit);
}
