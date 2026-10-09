package com.zpantry.pantry.service;

import com.zpantry.pantry.persistence.PantryItemRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Soft-deletes items only after they have been expired for a full 24 hours. */
@Service
public class ExpiredPantryCleanupService {
    private final PantryItemRepository pantry;
    public ExpiredPantryCleanupService(PantryItemRepository pantry) { this.pantry = pantry; }
    @Transactional
    public int removeExpiredMoreThanOneDayAgo() {
        Instant now = Instant.now();
        return pantry.softDeleteExpiredBefore(now.minus(Duration.ofDays(1)), now);
    }
    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void scheduledCleanup() { removeExpiredMoreThanOneDayAgo(); }
}
