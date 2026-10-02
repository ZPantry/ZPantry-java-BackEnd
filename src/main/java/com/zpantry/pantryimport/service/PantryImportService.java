package com.zpantry.pantryimport.service;

import com.zpantry.integration.ai.AiClient;
import com.zpantry.pantry.api.PantryDtos.UpsertPantryItemRequest;
import com.zpantry.pantry.service.PantryService;
import com.zpantry.pantryimport.api.PantryImportDtos.*;

import java.math.BigDecimal;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PantryImportService {
    private static final Logger log = LoggerFactory.getLogger(PantryImportService.class);
    private final AiClient ai;
    private final CatalogPreviewFactory previews;
    private final PantryService pantry;

    public PantryImportService(AiClient a, CatalogPreviewFactory previews, PantryService p) {
        ai = a;
        this.previews = previews;
        pantry = p;
    }

    public PantryImportPreviewResponse analyze(SourceType type, MultipartFile image) {
        try {
            var response = ai.postImage(type == SourceType.RECEIPT ? "/ai/analyze-receipt" : "/ai/recognize-food-image", image.getBytes(), image.getOriginalFilename(), image.getContentType());
            Object data = response.get("data");
            if (!(data instanceof Map<?, ?> map) || !(map.get("items") instanceof List<?> items))
                throw new IllegalStateException("AI returned incomplete analysis");
            var result = new ArrayList<PantryImportPreviewItem>();
            int unmatched = 0;
            for (Object x : items)
                if (x instanceof Map<?, ?> row && !Boolean.FALSE.equals(row.get("food"))) {
                    String raw = rawName(row);
                    if (raw == null) { unmatched++; continue; }
                    var preview = previews.resolved(raw, num(row.get("quantity")), num(row.get("price")), num(row.get("confidence")));
                    if (preview.isPresent()) result.add(preview.get()); else unmatched++;
                }
            var warnings = new ArrayList<String>();
            if (result.isEmpty()) warnings.add("No catalog ingredients were detected.");
            else if (unmatched > 0) warnings.add("Some detected items were not found in the ingredient catalog and were omitted.");
            return new PantryImportPreviewResponse(type, List.copyOf(result), List.copyOf(warnings));
        } catch (Exception e) {
            log.warn("Image analysis request failed: {}", e.getMessage());
            throw new IllegalStateException("Image analysis is currently unavailable");
        }
    }

    @Transactional
    public void confirm(UUID userId, ConfirmPantryImportRequest request) {
        for (var item : request.items()) pantry.validateImportItem(item.ingredientId(), item.quantity(), item.unit());
        for (var item : request.items())
            pantry.upsert(userId, new UpsertPantryItemRequest(item.ingredientId(), item.quantity(), item.unit(), null, null, null));
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
}
