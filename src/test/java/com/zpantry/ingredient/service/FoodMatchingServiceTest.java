package com.zpantry.ingredient.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.zpantry.ingredient.domain.IngredientAliasEntity;
import com.zpantry.ingredient.domain.IngredientEntity;
import com.zpantry.ingredient.persistence.IngredientAliasRepository;
import com.zpantry.ingredient.persistence.IngredientRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

class FoodMatchingServiceTest {
    private final IngredientRepository ingredients = org.mockito.Mockito.mock(IngredientRepository.class);
    private final IngredientAliasRepository aliases = org.mockito.Mockito.mock(IngredientAliasRepository.class);
    private final FoodMatchingService service = new FoodMatchingService(ingredients, aliases);

    @Test
    void resolvesExactAliasAndDoesNotUseFuzzyMatching() {
        IngredientEntity rice = new IngredientEntity("Rice");
        UUID riceId = UUID.randomUUID();
        IngredientAliasEntity alias = new IngredientAliasEntity(riceId, "White rice", "white rice");
        when(ingredients.findAllByDeletedFalse(org.springframework.data.domain.Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(rice)));
        when(aliases.findAllByDeletedFalse()).thenReturn(List.of(alias));
        when(ingredients.findByIdAndDeletedFalse(riceId)).thenReturn(Optional.of(rice));

        var resolved = service.resolve("White rice");

        assertThat(resolved.isResolved()).isTrue();
        assertThat(resolved.canonicalFood().name).isEqualTo("Rice");

        when(aliases.findAllByDeletedFalse()).thenReturn(List.of());
        assertThat(service.resolve("riceish").isResolved()).isFalse();
    }
}
