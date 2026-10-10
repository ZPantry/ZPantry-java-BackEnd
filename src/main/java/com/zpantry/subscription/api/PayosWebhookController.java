package com.zpantry.subscription.api;

import com.zpantry.subscription.service.PayosGateway;
import com.zpantry.subscription.service.SubscriptionService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** Public PayOS callback; its HMAC signature is the sole authentication mechanism. */
@RestController
@RequestMapping("/api/payment/payos")
public class PayosWebhookController {
    private final PayosGateway payos;
    private final SubscriptionService subscriptions;
    public PayosWebhookController(PayosGateway payos, SubscriptionService subscriptions) { this.payos=payos; this.subscriptions=subscriptions; }

    @PostMapping(value="/webhook", consumes="application/json")
    ResponseEntity<Map<String,String>> webhook(@RequestBody JsonNode payload) {
        JsonNode data=payload.path("data");
        if(!payos.isValidWebhook(data,payload.path("signature").asText(null))||!valid(data))return ResponseEntity.badRequest().body(Map.of("message","Invalid PayOS webhook."));
        boolean paid=payload.path("success").asBoolean(false)&&"00".equals(payload.path("code").asText())&&"00".equals(data.path("code").asText());
        subscriptions.processPayosWebhook(data.path("orderCode").asLong(),data.path("amount").asLong(),paid,data.path("reference").asText(null),data.path("desc").asText(payload.path("desc").asText(null)));
        return ResponseEntity.ok(Map.of("message","OK"));
    }
    private boolean valid(JsonNode data) { return data.isObject()&&data.path("orderCode").canConvertToLong()&&data.path("orderCode").asLong()>0&&data.path("amount").canConvertToLong()&&data.path("amount").asLong()>0; }
}
