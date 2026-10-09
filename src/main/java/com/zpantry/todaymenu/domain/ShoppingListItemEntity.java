package com.zpantry.todaymenu.domain;

import com.zpantry.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "shopping_list_items")
public class ShoppingListItemEntity extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    public UUID userId;
    @Column(name = "today_menu_item_id", nullable = false)
    public UUID todayMenuItemId;
    @Column(name = "ingredient_id", nullable = false)
    public UUID ingredientId;
    @Column(name = "ingredient_name", nullable = false)
    public String ingredientName;
    @Column(nullable = false, precision = 18, scale = 4)
    public BigDecimal quantity;
    @Column(nullable = false)
    public String unit = "";
    @Column(nullable = false)
    public String status = "PENDING";
}
