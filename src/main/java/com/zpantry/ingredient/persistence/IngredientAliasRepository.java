package com.zpantry.ingredient.persistence;
import com.zpantry.ingredient.domain.IngredientAliasEntity; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface IngredientAliasRepository extends JpaRepository<IngredientAliasEntity,UUID>{ List<IngredientAliasEntity> findAllByNormalizedAliasNameAndDeletedFalse(String name); }
