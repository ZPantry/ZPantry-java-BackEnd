package com.zpantry.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.zpantry.ZPantryBackendApplication;
import com.zpantry.foundation.IsolatedPostgres;
import com.zpantry.subscription.service.SubscriptionService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(classes = ZPantryBackendApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false", "spring.flyway.clean-disabled=true",
        "spring.sql.init.mode=never", "spring.jpa.open-in-view=false"})
@Import(IsolatedPostgres.class)
class SubscriptionPaymentIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired SubscriptionService subscriptions;

    @Test
    void verifiedPaymentActivatesOnePlanAndWebhookReplayIsIdempotent() {
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        long orderCode = 1_726_000_000_001L;
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("INSERT INTO payment_transactions(id,created_at,user_id,provider,merchant_order_id,amount_vnd,status) VALUES(?,?,?,?,?,?,?)",
                paymentId, now, userId, "PAYOS", "ZP-" + orderCode, 49_000, "PENDING");

        assertThat(subscriptions.processPayosWebhook(orderCode, 49_000, true, "bank-reference", null))
                .isEqualTo(SubscriptionService.PaymentWebhookOutcome.PAID);
        assertThat(subscriptions.processPayosWebhook(orderCode, 49_000, true, "bank-reference", null))
                .isEqualTo(SubscriptionService.PaymentWebhookOutcome.ALREADY_PROCESSED);
        assertThat(jdbc.queryForObject("SELECT status FROM payment_transactions WHERE id=?", String.class, paymentId)).isEqualTo("PAID");
        assertThat(jdbc.queryForObject("SELECT provider_transaction_id FROM payment_transactions WHERE id=?", String.class, paymentId)).isEqualTo("bank-reference");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_subscriptions WHERE user_id=? AND status='ACTIVE'", Integer.class, userId)).isEqualTo(1);
    }

    @Test
    void verifiedUnsuccessfulPaymentDoesNotActivateAPlan() {
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        long orderCode = 1_726_000_000_002L;
        jdbc.update("INSERT INTO payment_transactions(id,created_at,user_id,provider,merchant_order_id,amount_vnd,status) VALUES(?,?,?,?,?,?,?)",
                paymentId, Timestamp.from(Instant.now()), userId, "PAYOS", "ZP-" + orderCode, 49_000, "PENDING");

        assertThat(subscriptions.processPayosWebhook(orderCode, 49_000, false, null, "cancelled"))
                .isEqualTo(SubscriptionService.PaymentWebhookOutcome.FAILED);
        assertThat(jdbc.queryForObject("SELECT status FROM payment_transactions WHERE id=?", String.class, paymentId)).isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_subscriptions WHERE user_id=?", Integer.class, userId)).isZero();
    }
}
