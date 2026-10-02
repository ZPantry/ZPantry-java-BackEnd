package com.zpantry.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.zpantry.user.security.LegacyClaimConverter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class LegacyClaimConverterTest {
    private static final String NAME_ID =
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier";
    private final LegacyClaimConverter converter = new LegacyClaimConverter();

    @Test
    void explicitUserIdWinsWhenEmailSubWasMappedToNameIdentifier() {
        UUID owner = UUID.randomUUID();
        Jwt jwt = jwt(Map.of(
                "sub", "owner@test.dev",
                "userId", owner.toString(),
                NAME_ID, List.of("owner@test.dev", owner.toString()),
                "role", "user",
                "jti", "synthetic-jti"));
        var identity = converter.convert(jwt).orElseThrow();
        assertThat(identity.userId()).isEqualTo(owner);
        assertThat(identity.email()).isEqualTo("owner@test.dev");
        assertThat(identity.roles()).containsExactly("user");
    }

    @Test
    void fallbackRequiresOneUnambiguousUuidAndNeverUsesEmailSub() {
        UUID owner = UUID.randomUUID();
        assertThat(converter.convert(jwt(Map.of("sub", "owner@test.dev", NAME_ID, owner.toString(), "jti", "x"))))
                .get().extracting(identity -> identity.userId()).isEqualTo(owner);
        assertThat(converter.convert(jwt(Map.of("sub", "owner@test.dev", NAME_ID,
                List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()), "jti", "x")))).isEmpty();
        assertThat(converter.convert(jwt(Map.of("sub", "owner@test.dev", "jti", "x")))).isEmpty();
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return new Jwt("synthetic", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "HS256"), claims);
    }
}
