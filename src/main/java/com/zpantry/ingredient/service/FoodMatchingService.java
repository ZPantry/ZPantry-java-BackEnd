package com.zpantry.ingredient.service;

import com.zpantry.ingredient.domain.IngredientAliasEntity;
import com.zpantry.ingredient.domain.IngredientEntity;
import com.zpantry.ingredient.persistence.IngredientAliasRepository;
import com.zpantry.ingredient.persistence.IngredientRepository;
import com.zpantry.pantryimport.api.PantryImportDtos.ResolverStatus;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * The sole catalog matching boundary. It never guesses from categories or fuzzy search:
 * exact canonical name, exact alias, then one unique canonical/alias phrase in a product label.
 */
@Service
public class FoodMatchingService {
    private final IngredientRepository ingredients;
    private final IngredientAliasRepository aliases;

    public FoodMatchingService(IngredientRepository ingredients, IngredientAliasRepository aliases) {
        this.ingredients = ingredients;
        this.aliases = aliases;
    }

    public ResolvedIngredient resolve(String rawName) {
        String normalized = FoodNameNormalizer.normalize(rawName);
        if (normalized.isBlank()) return ResolvedIngredient.unresolved(normalized, ResolverStatus.UNRESOLVED);

        List<IngredientEntity> foods = ingredients.findAllByDeletedFalse(Pageable.unpaged()).getContent();
        var canonical = foods.stream().filter(food -> normalized.equals(FoodNameNormalizer.normalize(food.normalizedName))).toList();
        if (canonical.size() == 1) return ResolvedIngredient.resolved(normalized, canonical.getFirst(), canonical.getFirst().normalizedName);
        if (canonical.size() > 1) return ResolvedIngredient.unresolved(normalized, ResolverStatus.AMBIGUOUS);

        var exactAliases = activeAliasFoods(aliases.findAllByDeletedFalse().stream()
                .filter(alias -> normalized.equals(FoodNameNormalizer.normalize(alias.normalizedAliasName))).toList());
        if (exactAliases.size() == 1) return ResolvedIngredient.resolved(normalized, exactAliases.values().iterator().next(), normalized);
        if (exactAliases.size() > 1) return ResolvedIngredient.unresolved(normalized, ResolverStatus.AMBIGUOUS);

        Map<UUID, Match> phraseMatches = new LinkedHashMap<>();
        foods.stream().filter(food -> contained(normalized, FoodNameNormalizer.normalize(food.normalizedName)))
                .forEach(food -> phraseMatches.put(food.getId(), new Match(food, food.normalizedName)));
        for (IngredientAliasEntity alias : aliases.findAllByDeletedFalse()) {
            if (!contained(normalized, FoodNameNormalizer.normalize(alias.normalizedAliasName))) continue;
            ingredients.findByIdAndDeletedFalse(alias.ingredientId)
                    .ifPresent(food -> phraseMatches.put(food.getId(), new Match(food, alias.normalizedAliasName)));
        }
        if (phraseMatches.size() == 1) {
            Match match = phraseMatches.values().iterator().next();
            return ResolvedIngredient.resolved(normalized, match.food(), match.name());
        }
        return ResolvedIngredient.unresolved(normalized,
                phraseMatches.isEmpty() ? ResolverStatus.UNRESOLVED : ResolverStatus.AMBIGUOUS);
    }

    private Map<UUID, IngredientEntity> activeAliasFoods(Collection<IngredientAliasEntity> rows) {
        Map<UUID, IngredientEntity> result = new LinkedHashMap<>();
        for (IngredientAliasEntity row : rows) ingredients.findByIdAndDeletedFalse(row.ingredientId)
                .ifPresent(food -> result.put(food.getId(), food));
        return result;
    }

    private static boolean contained(String input, String candidate) {
        if (candidate == null || candidate.isBlank()) return false;
        String escaped = java.util.regex.Pattern.quote(candidate);
        return input.matches(".*(?:^|\\s)" + escaped + "(?:$|\\s).*" );
    }

    private record Match(IngredientEntity food, String name) {
    }
}
