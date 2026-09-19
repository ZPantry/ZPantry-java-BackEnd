package com.zpantry.user.api;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String fullName, String email, String avatarUrl,
        boolean isEmailConfirmed, boolean isActive, String role,
        Instant createdAt, Instant updatedAt) {}
