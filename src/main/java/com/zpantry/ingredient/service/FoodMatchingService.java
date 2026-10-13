package com.zpantry.ingredient.service;

import com.zpantry.ingredient.domain.IngredientAliasEntity;
import com.zpantry.ingredient.domain.IngredientEntity;
import com.zpantry.ingredient.persistence.IngredientAliasRepository;
import com.zpantry.ingredient.persistence.IngredientRepository;
import com.zpantry.pantryimport.api.PantryImportDtos.ResolverStatus;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * The sole catalog matching boundary. Exact matching is preferred. A conservative fallback
 * corrects minor spelling/order differences only when one catalog item is clearly better than
 * every other candidate; it must never silently choose between comparable foods.
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
        if (phraseMatches.isEmpty()) {
            Match fuzzy = uniqueHighConfidenceMatch(normalized, foods, aliases.findAllByDeletedFalse());
            if (fuzzy != null) return ResolvedIngredient.resolved(normalized, fuzzy.food(), fuzzy.name());
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

    private Match uniqueHighConfidenceMatch(String input, List<IngredientEntity> foods,
                                            List<IngredientAliasEntity> aliasRows) {
        Map<IngredientEntity, ScoredMatch> bestByFood = new LinkedHashMap<>();
        for (IngredientEntity food : foods) {
            addCandidate(bestByFood, food, food.normalizedName, input);
        }
        for (IngredientAliasEntity alias : aliasRows) {
            ingredients.findByIdAndDeletedFalse(alias.ingredientId)
                    .ifPresent(food -> addCandidate(bestByFood, food, alias.normalizedAliasName, input));
        }
        List<ScoredMatch> ranked = bestByFood.values().stream()
                .sorted(Comparator.comparing(ScoredMatch::score).reversed())
                .toList();
        if (ranked.isEmpty() || ranked.getFirst().score() < 0.80d) return null;
        if (ranked.size() > 1 && ranked.getFirst().score() - ranked.get(1).score() < 0.08d) return null;
        ScoredMatch best = ranked.getFirst();
        return new Match(best.food(), best.name());
    }

    private static void addCandidate(Map<IngredientEntity, ScoredMatch> bestByFood, IngredientEntity food,
                                     String candidateName, String input) {
        if (candidateName == null) return;
        double score = similarity(input, FoodNameNormalizer.normalize(candidateName));
        ScoredMatch candidate = new ScoredMatch(food, candidateName, score);
        bestByFood.merge(food, candidate,
                (current, next) -> next.score() > current.score() ? next : current);
    }

    private static double similarity(String left, String right) {
        List<String> leftTokens = meaningfulTokens(left);
        List<String> rightTokens = meaningfulTokens(right);
        if (leftTokens.size() < 2 || rightTokens.size() < 2) return 0d;
        Set<String> leftSet = new HashSet<>(leftTokens);
        Set<String> rightSet = new HashSet<>(rightTokens);
        if (leftSet.equals(rightSet)) return 0.90d;
        Set<String> overlap = new HashSet<>(leftTokens);
        overlap.retainAll(rightTokens);
        double tokenScore = (2d * overlap.size()) / (leftTokens.size() + rightTokens.size());
        double editScore = 1d - ((double) levenshtein(left, right) / Math.max(left.length(), right.length()));
        return (tokenScore * 0.7d) + (editScore * 0.3d);
    }

    private static List<String> meaningfulTokens(String value) {
        return java.util.Arrays.stream(value.split(" "))
                .filter(token -> !token.isBlank())
                .filter(token -> !Set.of("tuoi", "dong", "lanh", "nguyen", "con", "cat", "lat", "fillet").contains(token))
                .toList();
    }

    private static int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        for (int column = 0; column <= right.length(); column++) previous[column] = column;
        for (int row = 1; row <= left.length(); row++) {
            int[] current = new int[right.length() + 1];
            current[0] = row;
            for (int column = 1; column <= right.length(); column++) {
                int replace = previous[column - 1] + (left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1);
                current[column] = Math.min(Math.min(current[column - 1] + 1, previous[column] + 1), replace);
            }
            previous = current;
        }
        return previous[right.length()];
    }

    private record Match(IngredientEntity food, String name) {
    }

    private record ScoredMatch(IngredientEntity food, String name, double score) {
    }
}
