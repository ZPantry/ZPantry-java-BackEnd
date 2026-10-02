package com.zpantry.foundation.development;

import com.zpantry.ingredient.domain.IngredientEntity;
import com.zpantry.ingredient.persistence.IngredientRepository;
import com.zpantry.recipe.domain.RecipeEntity;
import com.zpantry.recipe.domain.RecipeIngredientEntity;
import com.zpantry.recipe.persistence.RecipeIngredientRepository;
import com.zpantry.recipe.persistence.RecipeRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Development-only Vietnamese catalog for exercising Pantry and Recommendation V2. */
@Component
@Profile("dev")
public class VietnameseCatalogSeed implements ApplicationRunner {
    private final IngredientRepository ingredients;
    private final RecipeRepository recipes;
    private final RecipeIngredientRepository recipeIngredients;

    public VietnameseCatalogSeed(IngredientRepository ingredients, RecipeRepository recipes,
            RecipeIngredientRepository recipeIngredients) {
        this.ingredients = ingredients;
        this.recipes = recipes;
        this.recipeIngredients = recipeIngredients;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        Map<String, IngredientEntity> catalog = new HashMap<>();
        add(catalog, "Trứng gà", "protein", "quả", "EGG");
        add(catalog, "Thịt ức gà", "protein", "g", "");
        add(catalog, "Thịt heo", "protein", "g", "");
        add(catalog, "Thịt heo nạc", "protein", "g", "");
        add(catalog, "Cá hồi", "protein", "g", "FISH");
        add(catalog, "Tôm", "protein", "g", "SHELLFISH");
        add(catalog, "Đậu hũ", "protein", "g", "SOY");
        add(catalog, "Cơm trắng", "carbohydrate", "g", "");
        add(catalog, "Gạo", "carbohydrate", "g", "");
        add(catalog, "Bún tươi", "carbohydrate", "g", "WHEAT");
        add(catalog, "Phở", "carbohydrate", "g", "WHEAT");
        add(catalog, "Khoai lang", "carbohydrate", "g", "");
        add(catalog, "Cà chua", "vegetable", "g", "");
        add(catalog, "Rau cải xanh", "vegetable", "g", "");
        add(catalog, "Rau muống", "vegetable", "g", "");
        add(catalog, "Dưa leo", "vegetable", "g", "");
        add(catalog, "Hành tây", "vegetable", "g", "");
        add(catalog, "Hành lá", "vegetable", "g", "");
        add(catalog, "Tỏi", "seasoning", "g", "");
        add(catalog, "Nước mắm", "seasoning", "ml", "FISH");
        add(catalog, "Dầu ăn", "seasoning", "ml", "");
        add(catalog, "Đậu phộng", "nut", "g", "PEANUT");
        add(catalog, "Sữa tươi", "dairy", "ml", "MILK");
        add(catalog, "Táo", "fruit", "quả", "");
        // English aliases keep the existing AI test account's Pantry compatible with the seed catalog.
        add(catalog, "Egg", "protein", "piece", "EGG");
        add(catalog, "Tomato", "vegetable", "g", "");
        add(catalog, "Rice", "carbohydrate", "g", "");

        // Keep catalogue upserts independent from recipe seeding: a long-lived dev database
        // must receive newly introduced generic Pantry ingredients on its next restart.
        if (recipes.count() > 0) return;

        recipe(catalog, "Cơm gà rau cải", "Bữa giàu đạm với ức gà và rau cải.", "Cắt gà, áp chảo với tỏi; luộc rau; dùng cùng cơm.", "", "Thịt ức gà", "Rau cải xanh", "Cơm trắng", "Tỏi");
        recipe(catalog, "Trứng chiên cà chua", "Món nhanh từ trứng và cà chua.", "Đánh trứng, xào cà chua rồi chiên chín.", "EGG", "Trứng gà", "Cà chua", "Hành tây", "Dầu ăn");
        recipe(catalog, "Đậu hũ sốt cà chua", "Món chay giàu đạm thực vật.", "Áp chảo đậu hũ, nấu sốt cà chua cùng hành tây.", "SOY", "Đậu hũ", "Cà chua", "Hành tây", "Tỏi");
        recipe(catalog, "Cá hồi áp chảo khoai lang", "Cá hồi và khoai lang cân bằng dinh dưỡng.", "Áp chảo cá hồi, hấp khoai lang và ăn kèm dưa leo.", "FISH", "Cá hồi", "Khoai lang", "Dưa leo", "Tỏi");
        recipe(catalog, "Canh rau muống thịt nạc", "Canh rau muống và thịt nạc.", "Nấu thịt nạc với nước, cho rau muống vào và nêm vừa ăn.", "", "Thịt heo nạc", "Rau muống", "Nước mắm", "Tỏi");
        recipe(catalog, "Bún tôm rau cải", "Bún với tôm và rau xanh.", "Luộc bún, xào tôm với tỏi, thêm rau cải và nước mắm.", "SHELLFISH,WHEAT,FISH", "Bún tươi", "Tôm", "Rau cải xanh", "Tỏi", "Nước mắm");
        recipe(catalog, "Phở gà", "Phở gà đơn giản.", "Luộc phở, làm nóng nước dùng gà, thêm rau cải.", "WHEAT", "Phở", "Thịt ức gà", "Rau cải xanh", "Hành tây");
        recipe(catalog, "Cơm thịt nạc xào rau", "Cơm thịt nạc và rau xào.", "Xào thịt với tỏi, thêm rau cải, ăn cùng cơm.", "", "Thịt heo nạc", "Rau cải xanh", "Cơm trắng", "Tỏi");
        recipe(catalog, "Egg Tomato Rice Bowl", "Món mẫu khớp trực tiếp với Pantry test.", "Nấu cơm, chiên trứng và xào cà chua.", "EGG", "Egg", "Tomato", "Rice");
    }

    private void add(Map<String, IngredientEntity> catalog, String name, String category, String unit, String allergens) {
        String normalized = name.toLowerCase();
        IngredientEntity ingredient = ingredients.findByNormalizedNameAndDeletedFalse(normalized).orElseGet(() -> {
            IngredientEntity created = new IngredientEntity(name);
            created.category = category;
            created.unit = unit;
            created.allergens = allergens;
            return ingredients.save(created);
        });
        if (ingredient.defaultQuantity == null) {
            ingredient.defaultQuantity = defaultQuantity(unit);
            ingredient = ingredients.save(ingredient);
        }
        catalog.put(name, ingredient);
    }

    private static BigDecimal defaultQuantity(String unit) {
        return switch (unit) {
            case "g" -> new BigDecimal("100");
            case "ml" -> new BigDecimal("100");
            default -> BigDecimal.ONE;
        };
    }

    private void recipe(Map<String, IngredientEntity> catalog, String name, String description, String instructions,
            String allergens, String... ingredientNames) {
        RecipeEntity recipe = new RecipeEntity(name);
        recipe.description = description;
        recipe.instructionText = instructions;
        recipe.allergens = allergens;
        recipe.cookingTimeMinutes = 20;
        recipe.difficulty = "easy";
        recipe.servingSize = 1;
        recipe.sourceType = "development-seed";
        recipe = recipes.save(recipe);
        for (String ingredientName : ingredientNames) {
            IngredientEntity ingredient = catalog.get(ingredientName);
            recipeIngredients.save(new RecipeIngredientEntity(recipe.getId(), ingredient.getId(), BigDecimal.ONE,
                    ingredient.unit, true, null));
        }
    }
}
