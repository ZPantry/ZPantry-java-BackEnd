package com.zpantry.ingredient.persistence;

import com.zpantry.ingredient.domain.IngredientEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IngredientRepository extends JpaRepository<IngredientEntity, UUID> {
    Page<IngredientEntity> findAllByDeletedFalse(Pageable pageable);

    @Query("""
            select ingredient from IngredientEntity ingredient
            where ingredient.deleted = false
              and (lower(ingredient.name) like concat('%', :search, '%')
                or lower(ingredient.normalizedName) like concat('%', :search, '%')
                or lower(coalesce(ingredient.category, '')) like concat('%', :search, '%'))
            """)
    Page<IngredientEntity> searchActive(@Param("search") String search, Pageable pageable);

    Optional<IngredientEntity> findByIdAndDeletedFalse(UUID id);

    boolean existsByNormalizedNameAndDeletedFalse(String normalizedName);

    boolean existsByNormalizedNameAndDeletedFalseAndIdNot(String normalizedName, UUID id);
}
