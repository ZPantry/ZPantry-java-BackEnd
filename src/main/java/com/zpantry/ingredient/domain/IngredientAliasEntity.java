package com.zpantry.ingredient.domain;

import com.zpantry.common.persistence.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "ingredient_aliases")
public class IngredientAliasEntity extends BaseEntity {
    @Column(name = "ingredient_id", nullable = false)
    public UUID ingredientId;
    @Column(name = "alias_name", nullable = false, length = 200)
    public String aliasName;
    @Column(name = "normalized_alias_name", nullable = false, length = 200)
    public String normalizedAliasName;

    protected IngredientAliasEntity() {
    }

    public IngredientAliasEntity(UUID ingredientId, String aliasName, String normalizedAliasName) {
        this.ingredientId = ingredientId;
        this.aliasName = aliasName;
        this.normalizedAliasName = normalizedAliasName;
    }
}
