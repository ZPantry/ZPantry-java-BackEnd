package com.zpantry.user.security;

/** Boundary for future shared revocation storage; no permissive production implementation exists. */
@FunctionalInterface
public interface TokenRevocationChecker {
    boolean isRevoked(String jti);
}
