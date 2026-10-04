package com.zpantry.pantryimport.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists and atomically consumes the per-user image-analysis monthly allowance. */
@Service
public class ImageAnalysisQuotaService {
    private static final String CONSUME_SQL = """
            INSERT INTO image_analysis_monthly_usage (id, created_at, user_id, month_start, used_count)
            VALUES (?, ?, ?, ?, 1)
            ON CONFLICT (user_id, month_start) DO UPDATE
            SET used_count = image_analysis_monthly_usage.used_count + 1,
                updated_at = EXCLUDED.created_at
            WHERE image_analysis_monthly_usage.used_count < ?
            RETURNING used_count
            """;

    private final JdbcTemplate jdbc;
    private final int monthlyLimit;
    private final ZoneId zoneId;
    private final Clock clock;

    public ImageAnalysisQuotaService(JdbcTemplate jdbc,
            @Value("${zpantry.ai.image-analysis.monthly-limit:3}") int monthlyLimit,
            @Value("${zpantry.ai.image-analysis.zone-id:Asia/Ho_Chi_Minh}") String zoneId) {
        this(jdbc, monthlyLimit, ZoneId.of(zoneId), Clock.systemUTC());
    }

    ImageAnalysisQuotaService(JdbcTemplate jdbc, int monthlyLimit, ZoneId zoneId, Clock clock) {
        if (monthlyLimit < 1) throw new IllegalArgumentException("Image analysis monthly limit must be at least one.");
        this.jdbc = jdbc;
        this.monthlyLimit = monthlyLimit;
        this.zoneId = zoneId;
        this.clock = clock;
    }

    @Transactional
    public void consume(UUID userId) {
        LocalDate monthStart = YearMonth.now(clock.withZone(zoneId)).atDay(1);
        try {
            Integer used = jdbc.queryForObject(CONSUME_SQL, Integer.class, UUID.randomUUID(),
                    clock.instant(), userId, monthStart, monthlyLimit);
            if (used == null) throw new ImageAnalysisQuotaExceededException();
        } catch (EmptyResultDataAccessException exception) {
            throw new ImageAnalysisQuotaExceededException();
        }
    }
}
