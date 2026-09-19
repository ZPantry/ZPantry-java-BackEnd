package com.zpantry.authentication.security;

import com.zpantry.user.security.TokenRevocationChecker;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(TokenRevocationChecker.class)
public class LocalTokenRevocationService implements TokenRevocationChecker {
    private final ConcurrentHashMap<String, Instant> revoked = new ConcurrentHashMap<>();
    @Override public boolean isRevoked(String jti) {
        Instant expiry = revoked.get(jti);
        if (expiry == null) return false;
        if (!expiry.isAfter(Instant.now())) { revoked.remove(jti, expiry); return false; }
        return true;
    }
    public void revoke(String jti, Instant expiry) { revoked.put(jti, expiry); }
}
