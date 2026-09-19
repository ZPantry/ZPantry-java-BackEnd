package com.zpantry.user.security;

import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public final class LegacyJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final LegacyClaimConverter claims;

    public LegacyJwtAuthenticationConverter(LegacyClaimConverter claims) { this.claims = claims; }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<SimpleGrantedAuthority> authorities = claims.roles(jwt).stream()
                .map(String::toUpperCase)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        String principal = claims.convert(jwt).map(identity -> identity.userId().toString()).orElse(jwt.getSubject());
        return new JwtAuthenticationToken(jwt, authorities, principal);
    }
}
