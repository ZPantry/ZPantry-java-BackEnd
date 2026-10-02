package com.zpantry.recipe.persistence;

import com.zpantry.recipe.domain.RecipeEntity;

import java.util.*;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<RecipeEntity, UUID> {
    Page<RecipeEntity> findAllByDeletedFalse(Pageable p);

    Optional<RecipeEntity> findByIdAndDeletedFalse(UUID id);
}
