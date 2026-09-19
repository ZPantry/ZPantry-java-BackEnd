package com.zpantry.integration.ai; import java.util.*;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;import org.springframework.web.client.*;
@Component public class HttpAiClient implements AiClient{private final RestClient client;public HttpAiClient(@Value("${zpantry.ai.service-url:http://localhost:8000}")String url){client=RestClient.builder().baseUrl(url).build();}
 @SuppressWarnings("unchecked") public Map<String,Object> post(String p,Object r){try{return client.post().uri(p).body(r).retrieve().body(Map.class);}catch(RestClientException e){throw new AiIntegrationException("AI service request failed",e);}}
 public Optional<float[]> embedIngredient(UUID id,String n,String nn,String c){return embedding(post("/ai/embed-ingredient",Map.of("ingredientId",id,"name",n,"normalizedName",nn,"category",c==null?"":c)));}
 public Optional<float[]> embedRecipe(UUID id,String n,String d,List<String> i,String t){return embedding(post("/ai/embed-recipe",Map.of("recipeId",id,"name",n,"description",d==null?"":d,"ingredientNames",i,"instructionText",t==null?"":t)));}
 private Optional<float[]> embedding(Map<String,Object> m){Object data=m==null?null:m.get("data");if(data instanceof Map<?,?> dm&&dm.get("embedding") instanceof List<?> l){float[] v=new float[l.size()];for(int x=0;x<l.size();x++)v[x]=((Number)l.get(x)).floatValue();return Optional.of(v);}return Optional.empty();}
 public static class AiIntegrationException extends RuntimeException{public AiIntegrationException(String m,Throwable c){super(m,c);}}
}
