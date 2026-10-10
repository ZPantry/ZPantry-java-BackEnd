package com.zpantry.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class PayosGatewayTest {
    private static final String CHECKSUM_KEY = "test-checksum-key";
    private final ObjectMapper mapper = JsonMapper.builder().build();
    private final PayosGateway gateway = new PayosGateway(mapper, "client", "api", CHECKSUM_KEY,
            "https://example.test/payment/success", "https://example.test/payment/cancel", "https://example.test");

    @Test
    void acceptsWebhookSignatureBuiltFromAlphabeticallySortedData() throws Exception {
        var data = mapper.readTree("""
                {"reference":"bank-reference","orderCode":123,"amount":49000,"code":"00","desc":"success"}
                """);
        String signature = hmac("amount=49000&code=00&desc=success&orderCode=123&reference=bank-reference");

        assertThat(gateway.isValidWebhook(data, signature)).isTrue();
    }

    @Test
    void rejectsAlteredPayloadOrSignature() throws Exception {
        var data = mapper.readTree("{\"amount\":49000,\"orderCode\":123}");
        String signature = hmac("amount=49000&orderCode=123");

        assertThat(gateway.isValidWebhook(data, signature + "00")).isFalse();
        assertThat(gateway.isValidWebhook(mapper.readTree("{\"amount\":1,\"orderCode\":123}"), signature)).isFalse();
    }

    private String hmac(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(CHECKSUM_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return java.util.HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
