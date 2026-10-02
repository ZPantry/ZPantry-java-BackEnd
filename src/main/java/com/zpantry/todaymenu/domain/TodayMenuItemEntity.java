package com.zpantry.todaymenu.domain;

import com.zpantry.common.persistence.BaseEntity;
import jakarta.persistence.*;

import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "today_menu_items")
public class TodayMenuItemEntity extends BaseEntity {
    @Column(name = "user_id")
    public UUID userId;
    @Column(name = "meal_id")
    public UUID mealId;
    @Column(name = "recipe_id")
    public UUID recipeId;
    @Column(name = "meal_name")
    public String mealName;
    @Column(name = "meal_type")
    public String mealType;
    @Column(name = "serving_size")
    public Integer servingSize;
    @Column(name = "planned_date")
    public LocalDate plannedDate;
    public String status = "Planned";
    public String note;
    @Column(name = "cooked_at")
    public Instant cookedAt;
    @Column(name = "image_url")
    public String imageUrl;
    @Column(name = "image_public_id")
    public String imagePublicId;

    protected TodayMenuItemEntity() {
    }

    public TodayMenuItemEntity(UUID u) {
        userId = u;
    }
}
