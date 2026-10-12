package com.zpantry.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class FoodAllergenTest {

    @Test
    void parsesProductionCatalogAllergenAliases() {
        assertThat(FoodAllergen.parseStoredSet("MOLLUSCS;TREE_NUTS,WHEAT"))
                .isEqualTo(Set.of(FoodAllergen.MOLLUSCS, FoodAllergen.TREE_NUT, FoodAllergen.WHEAT));
    }
}
