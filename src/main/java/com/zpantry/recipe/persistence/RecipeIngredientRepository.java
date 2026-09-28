package com.zpantry.recipe.persistence;

import com.zpantry.recipe.domain.RecipeIngredientEntity;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredientEntity, UUID> {
    List<RecipeIngredientEntity> findAllByRecipeIdAndDeletedFalse(UUID id);
}
