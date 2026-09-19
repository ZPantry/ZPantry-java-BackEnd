package com.zpantry.authentication.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public final class AuthenticationDtos {
    private AuthenticationDtos() {}
    public record RegisterRequest(String fullName, @NotBlank @Email String email, @NotBlank String password) {}
    public record VerifyOtpRequest(String otpCode, String email) {}
    public record LoginRequest(String email, String password) {}
    public record RefreshTokenRequest(String refreshToken) {}
    public record AuthResponse(String accessToken, Instant expiresAt, String fullName,
            String email, String refreshToken, String role) {}
}
