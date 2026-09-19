package com.zpantry.user.security;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email, Set<String> roles, String jti) {
    public AuthenticatedUser {
        roles = Set.copyOf(roles);
    }
}
