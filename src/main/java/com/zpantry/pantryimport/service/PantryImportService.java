package com.zpantry.pantryimport.service;

import com.zpantry.integration.ai.AiClient;
import com.zpantry.pantry.service.PantryService;
import com.zpantry.pantryimport.api.PantryImportDtos.*;
import com.zpantry.pantryimport.domain.ExtractedIngredient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.core.NestedExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PantryImportService {
    private static final Logger log = LoggerFactory.getLogger(PantryImportService.class);
    private final AiClient ai;
    private final PantryIngredientPipeline pipeline;
    private final PantryService pantry;
    private final JdbcTemplate jdbc;
    private final UnmatchedIngredientLogger unmatched;

    public PantryImportService(AiClient a, PantryIngredientPipeline pipeline, PantryService p, JdbcTemplate jdbc, UnmatchedIngredientLogger unmatched) {
        ai = a;
        this.pipeline = pipeline;
        pantry = p;
        this.jdbc = jdbc;
        this.unmatched = unmatched;
    }

    public PantryImportPreviewResponse analyze(SourceType type, MultipartFile image) {
        try {
            var response = ai.postImage(type == SourceType.RECEIPT ? "/ai/analyze-receipt" : "/ai/recognize-food-image", image.getBytes(), image.getOriginalFilename(), image.getContentType());
            Object data = response.get("data");
            if (!(data instanceof Map<?, ?> map) || !(map.get("items") instanceof List<?> items))
                throw new IllegalStateException("AI returned incomplete analysis");
            var result = new ArrayList<PantryImportPreviewItem>();
            int unmatchedCount = 0;
            for (Object x : items)
                if (x instanceof Map<?, ?> row && !Boolean.FALSE.equals(row.get("food"))) {
                    String raw = rawName(row);
                    if (raw == null) { unmatchedCount++; continue; }
                    var preview = pipeline.resolve(new ExtractedIngredient(raw, text(row.get("canonicalHint")), num(row.get("quantity")), text(row.get("unit")),
                            num(row.get("price")), num(row.get("confidence")), flags(row.get("flags"))));
                    if (preview.isPresent()) { result.add(preview.get()); unmatched.record(preview.get()); } else unmatchedCount++;
                }
            var warnings = new ArrayList<String>();
            if (result.isEmpty()) warnings.add("No catalog ingredients were detected.");
            else if (unmatchedCount > 0) warnings.add("Some detected items need a catalog selection before they can be imported.");
            return new PantryImportPreviewResponse(type, List.copyOf(result), List.copyOf(warnings));
        } catch (com.zpantry.integration.ai.HttpAiClient.AiIntegrationException e) {
            log.warn("Image analysis request failed: status={}", e.status());
            throw new AiUnavailableException();
        } catch (Exception e) {
            log.warn("Image analysis request failed: {}", e.getMessage());
            throw new IllegalStateException("Image analysis is currently unavailable");
        }
    }

    /** Calls the unified provider endpoint, then reuses the same canonical-food pipeline as V1. */
    public UnifiedImageAnalysisResponse analyzeUnifiedImage(UUID userId, MultipartFile image, ExpectedImageType expectedType) {
        try {
            var response = ai.postImage("/ai/analyze-image", image.getBytes(), image.getOriginalFilename(), image.getContentType(), expectedType.name());
            Object data = response.get("data");
            if (!(data instanceof Map<?, ?> map)) throw new IllegalStateException("AI returned incomplete analysis");
            ImageType imageType = imageType(map.get("imageType"));
            if (imageType == ImageType.UNKNOWN) return storeAnalysis(userId, response(imageType, map, List.of(), List.of()));
            Object source = map.get("ingredients");
            if (!(source instanceof List<?> items)) throw new IllegalStateException("AI returned incomplete analysis");
            var result = new ArrayList<PantryImportPreviewItem>();
            int unmatchedCount = 0;
            for (Object value : items) if (value instanceof Map<?, ?> row && !Boolean.FALSE.equals(row.get("food"))) {
                String raw = rawName(row);
                if (raw == null) { unmatchedCount++; continue; }
                var preview = pipeline.resolve(new ExtractedIngredient(raw, text(row.get("canonicalHint")), num(row.get("quantity")), text(row.get("unit")), null, num(row.get("confidence")), flags(row.get("flags"))));
                if (preview.isPresent()) { result.add(preview.get()); unmatched.record(preview.get()); } else unmatchedCount++;
            }
            var warnings = new ArrayList<String>();
            if (result.isEmpty()) warnings.add("No catalog ingredients were detected.");
            else if (unmatchedCount > 0) warnings.add("Some detected items need a catalog selection before they can be imported.");
            return storeAnalysis(userId, response(imageType, map, List.copyOf(result), List.copyOf(warnings)));
        } catch (com.zpantry.integration.ai.HttpAiClient.AiIntegrationException exception) {
            log.warn("Unified image analysis request failed: status={}", exception.status());
            throw new AiUnavailableException();
        } catch (DataAccessException exception) {
            logDatabaseFailure(exception);
            throw new IllegalStateException("Image analysis is currently unavailable");
        } catch (Exception exception) {
            log.warn("Unified image analysis request failed: {}", exception.getMessage());
            throw new IllegalStateException("Image analysis is currently unavailable");
        }
    }

    public static final class AiUnavailableException extends RuntimeException { public AiUnavailableException() { super("AI_UNAVAILABLE"); } }

    @Transactional
    public void confirm(UUID userId, ConfirmPantryImportRequest request, String idempotencyKey) {
        if (request.analysisId() != null && jdbc.queryForObject(
                "select count(*) from pantry_import_analyses where id = ? and user_id = ? and expires_at > ?",
                Integer.class, request.analysisId(), userId, Instant.now()) == 0) {
            throw new IllegalArgumentException("Image analysis is expired or does not belong to this user.");
        }
        UUID operationId = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (idempotencyKey.length() > 100) throw new IllegalArgumentException("Idempotency-Key must not exceed 100 characters.");
            operationId = UUID.randomUUID();
            int inserted = jdbc.update("insert into pantry_import_idempotency (id, created_at, user_id, idempotency_key, analysis_id) values (?, ?, ?, ?, ?) on conflict (user_id, idempotency_key) do nothing",
                    operationId, Instant.now(), userId, idempotencyKey.trim(), request.analysisId());
            if (inserted == 0) return;
        }
        for (var item : request.items()) pantry.validateImportItem(userId, item.ingredientId(), item.quantity(), item.unit());
        for (var item : request.items())
            pantry.addFromImport(userId, item.ingredientId(), item.quantity(), item.unit());
        if (operationId != null) jdbc.update("update pantry_import_idempotency set response_json = ? where id = ?", "{\"status\":\"CONFIRMED\"}", operationId);
    }

    private static String rawName(Map<?, ?> item) {
        for (String field : List.of("rawName", "name", "item", "productName", "product")) {
            Object value = item.get(field);
            if (value instanceof String text && !text.isBlank()) return text.trim();
        }
        return null;
    }

    private static BigDecimal num(Object x) {
        try {
            return x instanceof Number n ? new BigDecimal(n.toString()) : x == null ? null : new BigDecimal(x.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    private static String text(Object value) { return value instanceof String valueText && !valueText.isBlank() ? valueText.trim() : null; }
    private static List<String> flags(Object value) {
        if (!(value instanceof List<?> values)) return List.of();
        return values.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    private static ImageType imageType(Object value) {
        if (!(value instanceof String text)) throw new IllegalStateException("AI returned invalid image type");
        try { return ImageType.valueOf(text.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new IllegalStateException("AI returned invalid image type"); }
    }

    private static UnifiedImageAnalysisResponse response(ImageType type, Map<?, ?> data,
                                                          List<PantryImportPreviewItem> ingredients, List<String> warnings) {
        BigDecimal confidence = num(data.get("imageTypeConfidence"));
        return new UnifiedImageAnalysisResponse(type, confidence, ingredients, warnings, UUID.randomUUID(), confidence,
                text(data.get("imageQuality")), confidence != null && confidence.compareTo(BigDecimal.valueOf(0.6)) < 0,
                text(data.get("reason")));
    }

    private UnifiedImageAnalysisResponse storeAnalysis(UUID userId, UnifiedImageAnalysisResponse response) {
        Instant now = Instant.now();
        jdbc.update("insert into pantry_import_analyses (id, created_at, user_id, image_type, status, expires_at) values (?, ?, ?, ?, ?, ?)",
                response.analysisId(), now, userId, response.imageType().name(), "ANALYZED", now.plusSeconds(30 * 60));
        return response;
    }

    /** Logs only database identity and PostgreSQL error metadata, never a JDBC URL or user data. */
    private void logDatabaseFailure(DataAccessException exception) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(exception);
        String sqlState = root instanceof java.sql.SQLException sql ? sql.getSQLState() : "unknown";
        try {
            var context = jdbc.queryForMap("select current_database() as database_name, current_schema() as schema_name, current_user as database_user");
            log.warn("Pantry analysis persistence failed: sqlState={} root={} database={} schema={} user={}",
                    sqlState, safeSummary(root.getMessage()), context.get("database_name"), context.get("schema_name"), context.get("database_user"));
        } catch (Exception contextFailure) {
            log.warn("Pantry analysis persistence failed: sqlState={} root={} (database context unavailable: {})",
                    sqlState, safeSummary(root.getMessage()), contextFailure.getClass().getSimpleName());
        }
    }

    private static String safeSummary(String value) {
        if (value == null || value.isBlank()) return "<empty>";
        String normalized = value.replaceAll("[\\r\\n]+", " ");
        return normalized.substring(0, Math.min(300, normalized.length()));
    }
}
