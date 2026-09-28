package com.zpantry.todaymenu.persistence;

import com.zpantry.todaymenu.domain.TodayMenuItemEntity;

import java.time.LocalDate;
import java.util.*;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodayMenuItemRepository extends JpaRepository<TodayMenuItemEntity, UUID> {
    Page<TodayMenuItemEntity> findAllByUserIdAndPlannedDateAndDeletedFalse(UUID u, LocalDate d, Pageable p);

    Optional<TodayMenuItemEntity> findByIdAndUserIdAndDeletedFalse(UUID id, UUID u);
}
