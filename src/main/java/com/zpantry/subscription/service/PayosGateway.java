package com.zpantry.subscription.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class PayosGateway {
    private final RestClient client; private final ObjectMapper objectMapper; private final String clientId, apiKey, checksumKey, returnUrl, cancelUrl;
    public PayosGateway(ObjectMapper objectMapper, @Value("${zpantry.payment.payos.client-id:}") String clientId, @Value("${zpantry.payment.payos.api-key:}") String apiKey, @Value("${zpantry.payment.payos.checksum-key:}") String checksumKey, @Value("${zpantry.payment.payos.return-url:}") String returnUrl, @Value("${zpantry.payment.payos.cancel-url:}") String cancelUrl, @Value("${zpantry.payment.payos.endpoint:https://api-merchant.payos.vn/v2/payment-requests}") String endpoint) { this.objectMapper=objectMapper; this.client=RestClient.builder().baseUrl(endpoint).build(); this.clientId=clientId;this.apiKey=apiKey;this.checksumKey=checksumKey;this.returnUrl=returnUrl;this.cancelUrl=cancelUrl; }
    public String create(long orderCode,long amount,String description) { if(clientId.isBlank()||apiKey.isBlank()||checksumKey.isBlank()||returnUrl.isBlank()||cancelUrl.isBlank())throw new IllegalStateException("PayOS is not configured."); String signature=hmac("amount="+amount+"&cancelUrl="+cancelUrl+"&description="+description+"&orderCode="+orderCode+"&returnUrl="+returnUrl); JsonNode response=client.post().contentType(MediaType.APPLICATION_JSON).header("x-client-id",clientId).header("x-api-key",apiKey).body(Map.of("orderCode",orderCode,"amount",amount,"description",description,"cancelUrl",cancelUrl,"returnUrl",returnUrl,"signature",signature,"items",java.util.List.of(Map.of("name","Z-Pantry Z-Plus","quantity",1,"price",amount)))).retrieve().body(JsonNode.class); if(response==null||!"00".equals(response.path("code").asString())||response.path("data").path("checkoutUrl").asText().isBlank())throw new IllegalStateException("PayOS could not create a payment link."); return response.path("data").path("checkoutUrl").asText(); }
    public boolean isValidWebhook(JsonNode data,String signature) { if(checksumKey.isBlank()||data==null||!data.isObject()||signature==null||signature.isBlank())return false; String expected=hmac(canonicalData(data)); return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),signature.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII)); }
    private String canonicalData(JsonNode data) { TreeMap<String,String> values=new TreeMap<>(); for(var entry:data.properties())values.put(entry.getKey(),canonicalValue(entry.getValue())); return values.entrySet().stream().map(entry->entry.getKey()+"="+entry.getValue()).collect(java.util.stream.Collectors.joining("&")); }
    private String canonicalValue(JsonNode value) { if(value==null||value.isNull()||"undefined".equals(value.asText())||"null".equals(value.asText()))return ""; if(!value.isValueNode())return objectMapper.writeValueAsString(sorted(value)); return value.asText(); }
    private JsonNode sorted(JsonNode node) { if(node.isArray()){var array=objectMapper.createArrayNode();node.forEach(value->array.add(sorted(value)));return array;} if(node.isObject()){var object=objectMapper.createObjectNode();TreeMap<String,JsonNode> fields=new TreeMap<>();node.properties().forEach(entry->fields.put(entry.getKey(),entry.getValue()));fields.forEach((name,value)->object.set(name,sorted(value)));return object;}return node; }
    private String hmac(String data) { try { Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(checksumKey.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8))); }catch(Exception e){throw new IllegalStateException("Cannot sign PayOS request.",e);} }
}
