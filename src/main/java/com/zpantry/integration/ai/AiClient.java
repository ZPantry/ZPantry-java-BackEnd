package com.zpantry.integration.ai; import java.util.*;
public interface AiClient{Optional<float[]> embedIngredient(UUID id,String name,String normalized,String category);Optional<float[]> embedRecipe(UUID id,String name,String description,List<String> ingredients,String instructions);Map<String,Object> post(String path,Object request);}
