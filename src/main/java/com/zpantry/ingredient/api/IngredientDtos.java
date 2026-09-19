package com.zpantry.ingredient.api; import java.math.BigDecimal;import java.util.UUID;import org.springframework.web.multipart.MultipartFile;
public final class IngredientDtos{private IngredientDtos(){} public record IngredientResponse(UUID id,String name,String normalizedName,String category,String unit,BigDecimal caloriesPerUnit,BigDecimal proteinPerUnit,BigDecimal fatPerUnit,BigDecimal carbPerUnit,String imageUrl,String gradientFrom,String gradientTo){}
 public record CreateIngredientRequest(String name,String category,String unit,BigDecimal caloriesPerUnit,BigDecimal proteinPerUnit,BigDecimal fatPerUnit,BigDecimal carbPerUnit,String imageUrl,String gradientFrom,String gradientTo){}
 public record UpdateIngredientRequest(String name,String category,String unit,BigDecimal caloriesPerUnit,BigDecimal proteinPerUnit,BigDecimal fatPerUnit,BigDecimal carbPerUnit,String imageUrl,String gradientFrom,String gradientTo){}
 public record IngredientFormRequest(String name,String category,String unit,BigDecimal caloriesPerUnit,BigDecimal proteinPerUnit,BigDecimal fatPerUnit,BigDecimal carbPerUnit,String imageUrl,String gradientFrom,String gradientTo,MultipartFile imageFile){}
}
