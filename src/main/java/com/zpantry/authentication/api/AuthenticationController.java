package com.zpantry.authentication.api;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.zpantry.authentication.service.AuthenticationFailure;
import com.zpantry.authentication.service.AuthenticationService;
import com.zpantry.authentication.service.GoogleAuthService;
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
    private final GoogleAuthService googleAuthService;

    public AuthenticationController(AuthenticationService service, GoogleAuthService googleAuthService) {
        this.service = service;
        this.googleAuthService = googleAuthService;
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

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest body, HttpServletRequest req) {
        try {
            service.forgotPassword(body);
            return ResponseEntity.ok(ok(null, "Nếu email tồn tại, mã OTP đặt lại mật khẩu đã được gửi.", req));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(fail("Không thể gửi mã OTP đặt lại mật khẩu.", req));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest body, HttpServletRequest req) {
        boolean success = service.resetPassword(body);
        return ResponseEntity.status(success ? 200 : 400).body(success
                ? ok(null, "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.", req)
                : fail("Mã OTP không hợp lệ, đã hết hạn hoặc mật khẩu xác nhận không khớp.", req));
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

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticateGoogle(@RequestBody GoogleLoginRequest body, HttpServletRequest req) {
        return auth(() -> {
            try {
                String idToken = body.idToken();
                GoogleIdToken.Payload payload = googleAuthService.verifyToken(idToken);
                
                return service.googleLogin(
                        payload.getEmail(),
                        (String) payload.get("name"),
                        (String) payload.get("picture")
                );
            } catch (Exception e) {
                throw new AuthenticationFailure("Xác thực Google thất bại: " + e.getMessage());
            }
        }, req);
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
