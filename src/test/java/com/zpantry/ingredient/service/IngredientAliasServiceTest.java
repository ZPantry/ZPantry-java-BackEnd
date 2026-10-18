package com.zpantry.ingredient.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zpantry.ingredient.api.IngredientDtos.CreateIngredientAliasRequest;
import com.zpantry.ingredient.domain.IngredientEntity;
import com.zpantry.ingredient.persistence.IngredientAliasRepository;
import com.zpantry.ingredient.persistence.IngredientRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class IngredientAliasServiceTest {
    @Test
    void skipsDuplicateAliasesInOneBatchAndSavesTheUniqueAlias() {
        IngredientRepository ingredients = Mockito.mock(IngredientRepository.class);
        IngredientAliasRepository aliases = Mockito.mock(IngredientAliasRepository.class);
        FoodMatchingService matching = Mockito.mock(FoodMatchingService.class);
        UUID ingredientId = UUID.randomUUID();
        when(ingredients.findByIdAndDeletedFalse(ingredientId)).thenReturn(Optional.of(new IngredientEntity("Rice")));
        when(aliases.saveAll(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = new IngredientAliasService(ingredients, aliases, matching).createBatch(ingredientId,
                List.of(new CreateIngredientAliasRequest("White Rice"), new CreateIngredientAliasRequest(" white   rice ")));

        assertThat(result.success()).isTrue();
        assertThat(result.data()).hasSize(1);
        assertThat(result.message()).contains("skipped: 1");
        verify(aliases).saveAll(org.mockito.ArgumentMatchers.any());
        verify(matching).invalidateCatalog();
    }
}
