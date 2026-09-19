package com.zpantry.user.service;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

@Component
public final class LegacyPasswordHasher {
    private static final int ITERATIONS = 1_000;
    private static final int SALT_BYTES = 16;
    private static final int SUBKEY_BYTES = 32;
    private final SecureRandom random;

    public LegacyPasswordHasher() { this(new SecureRandom()); }
    LegacyPasswordHasher(SecureRandom random) { this.random = random; }

    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] subkey = derive(password, salt);
        byte[] payload = new byte[1 + SALT_BYTES + SUBKEY_BYTES];
        System.arraycopy(salt, 0, payload, 1, SALT_BYTES);
        System.arraycopy(subkey, 0, payload, 1 + SALT_BYTES, SUBKEY_BYTES);
        return Base64.getEncoder().encodeToString(payload);
    }

    public boolean verify(String encodedHash, String password) {
        try {
            byte[] payload = Base64.getDecoder().decode(encodedHash);
            if (payload.length != 49 || payload[0] != 0) return false;
            byte[] salt = Arrays.copyOfRange(payload, 1, 17);
            byte[] expected = Arrays.copyOfRange(payload, 17, 49);
            return MessageDigest.isEqual(expected, derive(password, salt));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, SUBKEY_BYTES * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Required legacy password algorithm is unavailable", exception);
        } finally {
            spec.clearPassword();
        }
    }
}
