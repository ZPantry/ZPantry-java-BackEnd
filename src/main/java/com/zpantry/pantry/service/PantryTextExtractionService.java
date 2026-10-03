package com.zpantry.pantry.service;

import com.zpantry.pantryimport.domain.ExtractedIngredient;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Local, deterministic text segmentation. This component deliberately makes no network or AI call. */
@Service
public class PantryTextExtractionService {
    private static final Pattern ITEM = Pattern.compile(
            "^\\s*(?:(\\d+(?:[.,]\\d+)?)\\s*(?:kg|g|ml|l|cái|quả|miếng|bó|chai|hộp)?\\s+)?(.+?)\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern LEADING_CONTEXT = Pattern.compile(
            "^(?:tôi có|mình có|nhà còn|trong tủ còn|còn|mua)\\s+", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public List<ExtractedIngredient> extract(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<ExtractedIngredient> extracted = new ArrayList<>();
        for (String phrase : text.split("[,;\\n]|\\s+(?:và|va|and)\\s+")) {
            String candidate = LEADING_CONTEXT.matcher(phrase.trim()).replaceFirst("");
            if (candidate.isBlank()) continue;
            Matcher matcher = ITEM.matcher(candidate);
            if (!matcher.matches()) continue;
            BigDecimal quantity = number(matcher.group(1));
            String name = matcher.group(2).trim();
            if (!name.isBlank()) extracted.add(new ExtractedIngredient(name, quantity, null, null));
        }
        return List.copyOf(extracted);
    }

    private static BigDecimal number(String value) {
        if (value == null) return null;
        try {
            return new BigDecimal(value.replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
