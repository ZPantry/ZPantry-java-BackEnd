package com.zpantry.user.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public final class LegacyClaimConverter {
    static final String NAME_IDENTIFIER =
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier";
    static final String ROLE =
            "http://schemas.microsoft.com/ws/2008/06/identity/claims/role";

    public Optional<AuthenticatedUser> convert(Jwt jwt) {
        UUID userId = parseUuid(jwt.getClaim("userId")).orElseGet(() -> fallbackUuid(jwt.getClaims()).orElse(null));
        if (userId == null) return Optional.empty();
        String email = firstString(jwt.getClaim("email"));
        if (email == null) email = firstString(jwt.getClaim("sub"));
        return Optional.of(new AuthenticatedUser(userId, email, roles(jwt), firstString(jwt.getClaim("jti"))));
    }

    public Set<String> roles(Jwt jwt) {
        var rawRoles = new LinkedHashSet<String>();
        addStrings(rawRoles, jwt.getClaim("role"));
        addStrings(rawRoles, jwt.getClaim(ROLE));
        var roles = new LinkedHashSet<String>();
        rawRoles.stream().map(role -> role.trim().toLowerCase(Locale.ROOT))
                .filter(role -> !role.isEmpty()).forEach(roles::add);
        return Set.copyOf(roles);
    }

    private Optional<UUID> fallbackUuid(Map<String, Object> claims) {
        var values = new LinkedHashSet<UUID>();
        addUuids(values, claims.get(NAME_IDENTIFIER));
        addUuids(values, claims.get("nameid"));
        return values.size() == 1 ? Optional.of(values.iterator().next()) : Optional.empty();
    }

    private static Optional<UUID> parseUuid(Object value) {
        String candidate = firstString(value);
        if (candidate == null) return Optional.empty();
        try { return Optional.of(UUID.fromString(candidate)); }
        catch (IllegalArgumentException ignored) { return Optional.empty(); }
    }

    private static void addUuids(Set<UUID> target, Object value) {
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> parseUuid(item).ifPresent(target::add));
        } else {
            parseUuid(value).ifPresent(target::add);
        }
    }

    private static void addStrings(Set<String> target, Object value) {
        if (value instanceof Collection<?> collection) {
            collection.stream().map(String::valueOf).forEach(target::add);
        } else if (value != null) {
            target.add(String.valueOf(value));
        }
    }

    private static String firstString(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream().findFirst().map(String::valueOf).orElse(null);
        }
        return value == null ? null : String.valueOf(value);
    }
}
