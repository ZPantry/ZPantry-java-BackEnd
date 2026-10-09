package com.zpantry.pantry.persistence;

import com.zpantry.pantry.domain.PantryItemEntity;

import java.util.*;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface PantryItemRepository extends JpaRepository<PantryItemEntity, UUID> {
    Page<PantryItemEntity> findAllByUserIdAndDeletedFalse(UUID u, Pageable p);

    List<PantryItemEntity> findAllByUserIdAndDeletedFalseOrderByExpiredAtAscCreatedAtAsc(UUID u);

    Optional<PantryItemEntity> findByUserIdAndIngredientIdAndDeletedFalse(UUID u, UUID i);

    Optional<PantryItemEntity> findByIdAndUserIdAndDeletedFalse(UUID id, UUID u);

    @Modifying
    @Query("update PantryItemEntity p set p.deleted = true, p.deletedAt = :deletedAt where p.deleted = false and p.expiredAt is not null and p.expiredAt < :cutoff")
    int softDeleteExpiredBefore(@Param("cutoff") Instant cutoff, @Param("deletedAt") Instant deletedAt);
}
