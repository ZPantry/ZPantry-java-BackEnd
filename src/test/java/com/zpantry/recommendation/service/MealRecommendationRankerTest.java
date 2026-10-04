package com.zpantry.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MealRecommendationRankerTest {
    private final MealRecommendationRanker ranker = new MealRecommendationRanker();

    @Test
    void prioritizesRecipeThatUsesExpiringPantryIngredients() {
        UUID milk = UUID.randomUUID();
        UUID bread = UUID.randomUUID();
        var useMilk = candidate("Milk toast", ingredient(milk, "Milk"));
        var useBread = candidate("Bread plate", ingredient(bread, "Bread"));

        var ranked = ranker.rank(List.of(useBread, useMilk), Set.of(milk, bread), Set.of(milk), 10);

        assertThat(ranked).extracting(MealRecommendationRanker.RankedRecipe::recipeName)
                .containsExactly("Milk toast", "Bread plate");
        assertThat(ranked.getFirst().expiringSoonIngredients()).containsExactly("Milk");
    }

    @Test
    void penalizesMissingRequiredIngredientsAndReturnsThemForExplanation() {
        UUID egg = UUID.randomUUID();
        UUID rice = UUID.randomUUID();
        var candidate = candidate("Egg rice", ingredient(egg, "Egg"), ingredient(rice, "Rice"));

        var ranked = ranker.rank(List.of(candidate), Set.of(egg), Set.of(), 10).getFirst();

        assertThat(ranked.score()).isEqualTo(20);
        assertThat(ranked.pantryMatchRatio()).isEqualTo(0.5);
        assertThat(ranked.missingIngredients()).containsExactly("Rice");
    }

    private static MealRecommendationRanker.RecipeCandidate candidate(String name,
                                                                        MealRecommendationRanker.RecipeIngredient... ingredients) {
        return new MealRecommendationRanker.RecipeCandidate(UUID.randomUUID(), name, 15, List.of(ingredients));
    }

    private static MealRecommendationRanker.RecipeIngredient ingredient(UUID id, String name) {
        return new MealRecommendationRanker.RecipeIngredient(id, name, true);
    }
}
