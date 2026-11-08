package com.zpantry.analytics.api;

import static com.zpantry.analytics.api.AnalyticsDtos.*;
import com.zpantry.analytics.service.AnalyticsService;
import com.zpantry.common.api.ApiResponse;
import com.zpantry.user.security.AuthenticatedUserResolver;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/analytics")
public class AnalyticsController {
    private final AnalyticsService service;
    private final AuthenticatedUserResolver users;
    public AnalyticsController(AnalyticsService service, AuthenticatedUserResolver users) { this.service = service; this.users = users; }
    private UUID userId(Authentication authentication) { return users.resolve(authentication).orElseThrow().userId(); }
    @PostMapping("/events")
    public ApiResponse<Void> track(Authentication authentication, @Valid @RequestBody TrackEventRequest request) {
        service.track(userId(authentication), request);
        return new ApiResponse<>(true, "Event recorded.", null, null, "", Instant.now());
    }
    @PostMapping("/ai-recognition/{analysisId}/feedback")
    public ApiResponse<RecognitionFeedbackResponse> feedback(Authentication authentication, @PathVariable UUID analysisId,
            @Valid @RequestBody RecognitionFeedbackRequest request) {
        return new ApiResponse<>(true, "Recognition feedback recorded.", service.feedback(userId(authentication), analysisId, request), null, "", Instant.now());
    }
}
