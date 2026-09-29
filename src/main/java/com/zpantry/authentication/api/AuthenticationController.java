package com.zpantry.authentication.api;

import com.zpantry.authentication.service.AuthenticationFailure;
import com.zpantry.authentication.service.AuthenticationService;
import com.zpantry.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import static com.zpantry.authentication.api.AuthenticationDtos.*;

@RestController
@RequestMapping("/api/Auth")
public class AuthenticationController {
    private final AuthenticationService service;

    public AuthenticationController(AuthenticationService service) {
        this.service = service;
    }

    private static <T> ApiResponse<T> ok(T data, String message, HttpServletRequest r) {
        return new ApiResponse<>(true, message, data, null, r.getRequestId(), Instant.now());
    }

    private static <T> ApiResponse<T> fail(String message, HttpServletRequest r) {
        return new ApiResponse<>(false, message, null, null, r.getRequestId(), Instant.now());
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> register(@Valid @RequestBody RegisterRequest body, HttpServletRequest req) {
        try {
            service.register(body);
            return ResponseEntity.ok(ok(null, "Đăng ký thành công! Vui lòng kiểm tra Gmail để nhận mã OTP xác thực.", req));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(fail(e.getMessage(), req));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(fail(e.getMessage(), req));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Object>> verify(@RequestBody VerifyOtpRequest body, HttpServletRequest req) {
        boolean success = service.verify(body);
        return ResponseEntity.status(success ? 200 : 400).body(success ? ok(null, "Xác thực tài khoản thành công! Bạn hiện đã có thể đăng nhập.", req) : fail("Mã OTP không chính xác, đã hết hạn hoặc tài khoản đã được xác thực trước đó.", req));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest body, HttpServletRequest req) {
        return auth(() -> service.login(body), req);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody RefreshTokenRequest body, HttpServletRequest req) {
        return auth(() -> service.refresh(body), req);
    }

    @PostMapping("/logout")
    public ApiResponse<Object> logout(JwtAuthenticationToken auth, HttpServletRequest req) {
        var jwt = auth.getToken();
        service.logout(jwt.getClaimAsString("email"), jwt.getId(), jwt.getExpiresAt());
        return ok(null, "Logout successful.", req);
    }

    private ResponseEntity<ApiResponse<AuthResponse>> auth(java.util.function.Supplier<AuthResponse> call, HttpServletRequest req) {
        try {
            return ResponseEntity.ok(ok(call.get(), "", req));
        } catch (AuthenticationFailure e) {
            return ResponseEntity.status(401).body(fail(e.getMessage(), req));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(fail(e.getMessage(), req));
        }
    }
}
