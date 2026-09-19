package com.zpantry.user.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public final class AuthenticatedUserResolver {
    private final LegacyClaimConverter claims;
    public AuthenticatedUserResolver(LegacyClaimConverter claims) { this.claims = claims; }

    public Optional<AuthenticatedUser> resolve(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()) {
            return Optional.empty();
        }
        return claims.convert(jwt.getToken());
    }
}
