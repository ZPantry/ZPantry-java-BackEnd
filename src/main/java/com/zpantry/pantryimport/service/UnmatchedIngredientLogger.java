package com.zpantry.pantryimport.service;

import com.zpantry.ingredient.service.FoodNameNormalizer;
import com.zpantry.pantryimport.api.PantryImportDtos.PantryImportPreviewItem;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** Best-effort operational feedback; it must never affect an image-analysis response. */
@Component
public class UnmatchedIngredientLogger {
    private static final Logger log = LoggerFactory.getLogger(UnmatchedIngredientLogger.class);
    private final JdbcTemplate jdbc;

    public UnmatchedIngredientLogger(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Async("unmatchedIngredientExecutor")
    public void record(PantryImportPreviewItem item) {
        if (item.ingredientId() != null || item.confidence() == null || item.confidence().compareTo(java.math.BigDecimal.valueOf(0.6)) < 0) return;
        try {
            String normalized = FoodNameNormalizer.normalize(item.rawName());
            if (normalized.isBlank()) return;
            String candidates = item.candidates().stream().map(candidate -> candidate.ingredientId() + ":" + candidate.name()).collect(java.util.stream.Collectors.joining(","));
            jdbc.update("""
                    insert into unmatched_ingredient_log (normalized_name, raw_name, attempted_names, candidate_summary, occurrence_count, status, first_seen_at, last_seen_at)
                    values (?, ?, ?, ?, 1, 'NEW', ?, ?)
                    on conflict (normalized_name) do update set occurrence_count = unmatched_ingredient_log.occurrence_count + 1,
                        raw_name = excluded.raw_name, attempted_names = excluded.attempted_names,
                        candidate_summary = excluded.candidate_summary, last_seen_at = excluded.last_seen_at
                    """, normalized, item.rawName(), item.normalizedName(), candidates, Instant.now(), Instant.now());
        } catch (RuntimeException exception) {
            log.warn("Unable to record unmatched pantry ingredient: {}", exception.getClass().getSimpleName());
        }
    }
}
