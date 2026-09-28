package com.zpantry.pantryimport.api;

import com.zpantry.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice(assignableTypes = PantryImportController.class)
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

    private ResponseEntity<ApiResponse<Void>> response(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiResponse<>(false, message, null, null, request.getRequestId(), Instant.now()));
    }
}
