package com.zpantry.recommendation.api;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.recommendation.api.RecommendationDtos.*;
import com.zpantry.recommendation.service.RecommendationService;
import com.zpantry.recommendation.service.PersonalizedRecommendationService;
import com.zpantry.user.security.AuthenticatedUserResolver;
import com.zpantry.subscription.service.SubscriptionService;

import java.util.*;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendationService s;
    private final AuthenticatedUserResolver ids;
    private final PersonalizedRecommendationService personalized;
    private final SubscriptionService subscriptions;

    public RecommendationController(RecommendationService s, AuthenticatedUserResolver i, PersonalizedRecommendationService personalized, SubscriptionService subscriptions) {
        this.s = s;
        ids = i;
        this.personalized = personalized;
        this.subscriptions = subscriptions;
    }

    private UUID id(Authentication a) {
        return ids.resolve(a).orElseThrow().userId();
    }

    @PostMapping("/meals")
    public ApiResponse<Map<String, Object>> meals(Authentication a, @RequestBody RecommendMealRequest r) {
        var result = s.recommend(id(a), r);
        if (result.success()) subscriptions.consume(id(a), SubscriptionService.MEAL_SUGGESTION);
        return result;
    }

    @PostMapping("/v2/meals")
    public ApiResponse<PersonalizedRecommendationResponse> personalized(Authentication a, @RequestBody(required = false) PersonalizedRecommendationRequest request) {
        var result = personalized.recommend(id(a), request);
        subscriptions.consume(id(a), SubscriptionService.MEAL_SUGGESTION);
        return new ApiResponse<>(true, "Personalized meal recommendations generated.", result, null, "", java.time.Instant.now());
    }

    @PostMapping("/missing-ingredients")
    public ApiResponse<Map<String, Object>> missing(Authentication a, @RequestBody Object r) {
        return s.missing(id(a), r);
    }

    @GetMapping("/meals/{mealId}/missing-ingredients")
    public ApiResponse<Map<String, Object>> check(Authentication a, @PathVariable UUID mealId) {
        return s.check(id(a), mealId);
    }

    @GetMapping("/{id}")
    public ApiResponse<Object> get(Authentication a, @PathVariable UUID id) {
        return s.get(id(a), id);
    }

    @PostMapping("/{id}/feedback")
    public ApiResponse<Object> feedback(Authentication a, @PathVariable UUID id, @RequestBody RecommendationFeedbackRequest r) {
        return s.feedback(id(a), id, r);
    }
}
