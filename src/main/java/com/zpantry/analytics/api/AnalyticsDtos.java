package com.zpantry.analytics.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

public final class AnalyticsDtos {
    private AnalyticsDtos() { }
    public record TrackEventRequest(
            @NotBlank @Pattern(regexp = "SCREEN_VIEW|AI_RECOGNITION_STARTED|AI_RECOGNITION_COMPLETED|AI_RECIPE_REQUESTED|AI_RECIPE_COMPLETED|RECIPE_SELECTED|PANTRY_ITEM_ADDED|MEAL_COMPLETED|UPGRADE_VIEWED|CHECKOUT_STARTED|PAYMENT_COMPLETED") String eventName,
            @Size(max = 100) String screenName,
            @Size(max = 100) String sessionId,
            Map<String, Object> properties) { }
    public record RecognitionFeedbackRequest(
            @NotBlank @Pattern(regexp = "LIKE|DISLIKE") String feedback,
            @Size(max = 300) String reason,
            @Size(max = 500) String correction) { }
    public record RecognitionFeedbackResponse(UUID analysisId, String feedback, boolean created) { }
}
