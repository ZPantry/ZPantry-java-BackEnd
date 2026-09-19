package com.zpantry.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.zpantry.user.service.LegacyPasswordHasher;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class LegacyPasswordHasherTest {
    private final LegacyPasswordHasher hasher = new LegacyPasswordHasher();

    @Test
    void verifiesSyntheticAspNetIdentityV2Vector() throws IOException {
        var vector = new Properties();
        try (var input = getClass().getResourceAsStream("/contracts/auth/password-hash-v0-vector.properties")) {
            vector.load(input);
        }
        assertThat(hasher.verify(vector.getProperty("hash"), vector.getProperty("password"))).isTrue();
        assertThat(hasher.verify(vector.getProperty("hash"), "wrong-synthetic-password")).isFalse();
    }

    @Test
    void generatesMarkerZeroFortyNineByteRandomSaltHashes() {
        String first = hasher.hash("Synthetic-Only-Password!");
        String second = hasher.hash("Synthetic-Only-Password!");
        byte[] payload = Base64.getDecoder().decode(first);
        assertThat(payload).hasSize(49);
        assertThat(payload[0]).isZero();
        assertThat(Arrays.copyOfRange(payload, 1, 17)).hasSize(16);
        assertThat(Arrays.copyOfRange(payload, 17, 49)).hasSize(32);
        assertThat(first).isNotEqualTo(second);
        assertThat(hasher.verify(first, "Synthetic-Only-Password!")).isTrue();
        assertThat(hasher.verify(second, "Synthetic-Only-Password!")).isTrue();
    }
}
