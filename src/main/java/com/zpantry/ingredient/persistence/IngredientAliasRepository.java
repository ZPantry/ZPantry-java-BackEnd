package com.zpantry.ingredient.persistence;

import com.zpantry.ingredient.domain.IngredientAliasEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientAliasRepository extends JpaRepository<IngredientAliasEntity, UUID> {
    List<IngredientAliasEntity> findAllByNormalizedAliasNameAndDeletedFalse(String name);

    List<IngredientAliasEntity> findAllByIngredientIdAndDeletedFalse(UUID ingredientId);

    List<IngredientAliasEntity> findAllByDeletedFalse();

    boolean existsByNormalizedAliasNameAndDeletedFalse(String normalizedAliasName);
}
