package com.zpantry.subscription.api;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class SubscriptionDtos {
    private SubscriptionDtos() {}
    public record PlanResponse(String code, String name, long priceVnd, int durationDays, String description) {}
    public record QuotaResponse(String feature, Integer limit, int used, Integer remaining, LocalDate resetsOn) {}
    public record SubscriptionResponse(String planCode, String status, Instant startedAt, Instant expiresAt, boolean autoRenew, List<QuotaResponse> quotas) {}
    public record CheckoutRequest(@NotBlank String provider) {}
    public record CheckoutResponse(String transactionId, String provider, String status, String checkoutUrl) {}
    public record PaymentResponse(String id, String provider, String status, long amountVnd, Instant createdAt, Instant paidAt) {}
}
