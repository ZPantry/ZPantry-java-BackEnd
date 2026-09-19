package com.zpantry.user.api;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.user.service.OwnerAuthorizationException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = UserController.class)
public class UserExceptionHandler {
    @ExceptionHandler(OwnerAuthorizationException.class)
    ResponseEntity<ApiResponse<UserResponse>> ownerForbidden(
            OwnerAuthorizationException exception, HttpServletRequest request) {
        var body = new ApiResponse<UserResponse>(false, exception.getMessage(), null, null,
                request.getRequestId(), Instant.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
}
