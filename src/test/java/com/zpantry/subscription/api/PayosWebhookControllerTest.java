package com.zpantry.subscription.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zpantry.subscription.service.PayosGateway;
import com.zpantry.subscription.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.json.JsonMapper;

class PayosWebhookControllerTest {
    private final PayosGateway payos = Mockito.mock(PayosGateway.class);
    private final SubscriptionService subscriptions = Mockito.mock(SubscriptionService.class);
    private final PayosWebhookController controller = new PayosWebhookController(payos, subscriptions);
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void rejectsUnsignedPayloadWithoutChangingPaymentState() throws Exception {
        var response = controller.webhook(mapper.readTree("""
                {"data":{"orderCode":123,"amount":49000},"signature":"invalid"}
                """));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(subscriptions, never()).processPayosWebhook(anyLong(), anyLong(), Mockito.anyBoolean(), anyString(), anyString());
    }

    @Test
    void recordsVerifiedUnsuccessfulWebhookAsFailed() throws Exception {
        var payload = mapper.readTree("""
                {"success":false,"code":"00","signature":"valid","data":{"orderCode":123,"amount":49000,"code":"01","desc":"cancelled"}}
                """);
        when(payos.isValidWebhook(payload.path("data"), "valid")).thenReturn(true);

        var response = controller.webhook(payload);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(subscriptions).processPayosWebhook(123, 49000, false, null, "cancelled");
    }
}
