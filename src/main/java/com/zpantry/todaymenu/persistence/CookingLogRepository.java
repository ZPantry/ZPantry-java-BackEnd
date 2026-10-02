package com.zpantry.todaymenu.persistence;

import com.zpantry.todaymenu.domain.CookingLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CookingLogRepository extends JpaRepository<CookingLogEntity, UUID> {
    Page<CookingLogEntity> findAllByUserIdAndDeletedFalse(UUID u, Pageable p);
}
