package com.zpantry.recommendation.api;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.recommendation.api.RecommendationDtos.*;
import com.zpantry.recommendation.service.RecommendationService;
import com.zpantry.recommendation.service.PersonalizedRecommendationService;
import com.zpantry.user.security.AuthenticatedUserResolver;

import java.util.*;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendationService s;
    private final AuthenticatedUserResolver ids;
    private final PersonalizedRecommendationService personalized;

    public RecommendationController(RecommendationService s, AuthenticatedUserResolver i, PersonalizedRecommendationService personalized) {
        this.s = s;
        ids = i;
        this.personalized = personalized;
    }

    private UUID id(Authentication a) {
        return ids.resolve(a).orElseThrow().userId();
    }

    @PostMapping("/meals")
    public ApiResponse<Map<String, Object>> meals(Authentication a, @RequestBody RecommendMealRequest r) {
        return s.recommend(id(a), r);
    }

    @PostMapping("/v2/meals")
    public ApiResponse<PersonalizedRecommendationResponse> personalized(Authentication a, @RequestBody(required = false) PersonalizedRecommendationRequest request) {
        return new ApiResponse<>(true, "Personalized meal recommendations generated.", personalized.recommend(id(a), request), null, "", java.time.Instant.now());
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
