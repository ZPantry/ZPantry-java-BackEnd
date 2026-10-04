package com.zpantry.authentication.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthenticationDtos {
    private AuthenticationDtos() {}
    public record RegisterRequest(String fullName, @NotBlank @Email String email, @NotBlank String password) {}
    public record VerifyOtpRequest(String otpCode, String email) {}
    public record LoginRequest(String email, String password) {}
    public record RefreshTokenRequest(String refreshToken) {}
    public record ForgotPasswordRequest(@NotBlank @Email String email) {}
    public record ResetPasswordRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "\\d{6}") String otpCode,
            @NotBlank @Size(min = 8, max = 200) String newPassword,
            @NotBlank String confirmPassword) {}
    public record AuthResponse(String accessToken, Instant expiresAt, String fullName,
            String email, String refreshToken, String role) {}
    public record GoogleLoginRequest(String idToken) {}
}
