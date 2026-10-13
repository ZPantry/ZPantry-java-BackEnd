package com.zpantry.pantryimport.api;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.pantry.api.NaturalLanguagePantryController;
import com.zpantry.subscription.api.SubscriptionController;
import com.zpantry.recommendation.api.RecommendationController;
import com.zpantry.subscription.service.SubscriptionLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice(assignableTypes = {PantryImportController.class, IngredientImageV2Controller.class, NaturalLanguagePantryController.class, SubscriptionController.class, RecommendationController.class})
public class PantryImportExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, MaxUploadSizeExceededException.class})
    ResponseEntity<ApiResponse<Void>> invalid(RuntimeException exception, HttpServletRequest request) {
        String message = exception instanceof MaxUploadSizeExceededException ? "Image must not exceed 10 MB." : exception.getMessage();
        return response(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiResponse<Void>> unavailable(IllegalStateException exception, HttpServletRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "Image analysis is currently unavailable.", request);
    }

    @ExceptionHandler(com.zpantry.pantryimport.service.PantryImportService.AiUnavailableException.class)
    ResponseEntity<java.util.Map<String, Object>> aiUnavailable(com.zpantry.pantryimport.service.PantryImportService.AiUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(java.util.Map.of(
                "error", java.util.Map.of("code", "AI_UNAVAILABLE", "message", "Hệ thống AI đang quá tải. Lượt của bạn không bị trừ.", "retryable", true),
                "quota", java.util.Map.of("charged", false)));
    }

    @ExceptionHandler(SubscriptionLimitExceededException.class)
    ResponseEntity<ApiResponse<Void>> subscriptionQuotaExceeded(SubscriptionLimitExceededException exception, HttpServletRequest request) {
        return response(HttpStatus.TOO_MANY_REQUESTS, "Usage quota reached. Upgrade to Z-Plus or wait for the next reset.", request);
    }

    private ResponseEntity<ApiResponse<Void>> response(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiResponse<>(false, message, null, null, request.getRequestId(), Instant.now()));
    }
}
