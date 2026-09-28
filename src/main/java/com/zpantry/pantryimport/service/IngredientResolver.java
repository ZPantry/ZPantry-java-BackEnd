package com.zpantry.pantryimport.service;

import com.zpantry.ingredient.domain.*;
import com.zpantry.ingredient.persistence.*;
import com.zpantry.pantryimport.api.PantryImportDtos.ResolverStatus;

import java.text.Normalizer;
import java.util.*;

import org.springframework.stereotype.Service;

@Service
public class IngredientResolver {
    private final IngredientRepository ingredients;
    private final IngredientAliasRepository aliases;

    public IngredientResolver(IngredientRepository i, IngredientAliasRepository a) {
        ingredients = i;
        aliases = a;
    }

    public String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    public Result resolve(String raw) {
        String normalized = normalize(raw);
        var matches = ingredients.searchActive(normalized, org.springframework.data.domain.Pageable.unpaged()).stream().filter(i -> i.normalizedName.equals(normalized)).toList();
        if (matches.size() == 1) return Result.resolved(normalized, matches.getFirst());
        var alias = aliases.findAllByNormalizedAliasNameAndDeletedFalse(normalized).stream().map(a -> ingredients.findByIdAndDeletedFalse(a.ingredientId).orElse(null)).filter(Objects::nonNull).toList();
        if (alias.size() == 1) return Result.resolved(normalized, alias.getFirst());
        return new Result(normalized, null, alias.size() + matches.size() > 1 ? ResolverStatus.AMBIGUOUS : ResolverStatus.UNRESOLVED);
    }

    public record Result(String normalizedName, IngredientEntity ingredient, ResolverStatus status) {
        static Result resolved(String n, IngredientEntity i) {
            return new Result(n, i, ResolverStatus.RESOLVED);
        }
    }
}
