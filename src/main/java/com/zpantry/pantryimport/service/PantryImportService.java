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

@Service
public class PantryImportService {
    private final AiClient ai;
    private final IngredientResolver resolver;
    private final PantryService pantry;

    public PantryImportService(AiClient a, IngredientResolver r, PantryService p) {
        ai = a;
        resolver = r;
        pantry = p;
    }

    public PantryImportPreviewResponse analyze(SourceType type, MultipartFile image) {
        try {
            var response = ai.postImage(type == SourceType.RECEIPT ? "/ai/analyze-receipt" : "/ai/recognize-food-image", image.getBytes(), image.getOriginalFilename(), image.getContentType());
            Object data = response.get("data");
            if (!(data instanceof Map<?, ?> map) || !(map.get("items") instanceof List<?> items))
                throw new IllegalStateException("AI returned incomplete analysis");
            var result = new ArrayList<PantryImportPreviewItem>();
            for (Object x : items)
                if (x instanceof Map<?, ?> row && !Boolean.FALSE.equals(row.get("food"))) {
                    String raw = String.valueOf(row.get("rawName"));
                    if (raw.equals("null")) continue;
                    var resolved = resolver.resolve(raw);
                    var i = resolved.ingredient();
                    result.add(new PantryImportPreviewItem(raw, resolved.normalizedName(), i == null ? null : i.getId(), i == null ? null : i.name, num(row.get("quantity")), str(row.get("unit")), num(row.get("price")), num(row.get("confidence")), resolved.status()));
                }
            return new PantryImportPreviewResponse(type, List.copyOf(result), result.isEmpty() ? List.of("No food ingredients were detected.") : List.of());
        } catch (Exception e) {
            throw new IllegalStateException("Image analysis is currently unavailable");
        }
    }

    @Transactional
    public void confirm(UUID userId, ConfirmPantryImportRequest request) {
        for (var item : request.items()) pantry.validateImportItem(item.ingredientId(), item.quantity(), item.unit());
        for (var item : request.items())
            pantry.upsert(userId, new UpsertPantryItemRequest(item.ingredientId(), item.quantity(), item.unit(), null, null, null));
    }

    private static String str(Object x) {
        return x == null ? null : String.valueOf(x);
    }

    private static BigDecimal num(Object x) {
        try {
            return x instanceof Number n ? new BigDecimal(n.toString()) : x == null ? null : new BigDecimal(x.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
