package com.zpantry.todaymenu.persistence;

import com.zpantry.todaymenu.domain.PantryUsageLogEntity;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PantryUsageLogRepository extends JpaRepository<PantryUsageLogEntity, UUID> {
    java.util.List<PantryUsageLogEntity> findAllByCookingLogIdAndDeletedFalseOrderByCreatedAtAsc(UUID cookingLogId);
}
