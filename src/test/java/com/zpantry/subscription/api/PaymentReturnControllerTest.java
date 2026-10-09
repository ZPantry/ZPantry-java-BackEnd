package com.zpantry.subscription.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentReturnControllerTest {
    private final PaymentReturnController controller = new PaymentReturnController();

    @Test
    void successPageOnlyReturnsTheUserToTheSuccessDeepLink() {
        var response = controller.success();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody()).contains("zpantry://payment/success");
        assertThat(response.getBody()).doesNotContain("transactionId");
    }

    @Test
    void cancelPageOnlyReturnsTheUserToTheCancelDeepLink() {
        var response = controller.cancel();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("zpantry://payment/cancel");
    }
}
