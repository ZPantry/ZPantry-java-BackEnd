package com.zpantry.user.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class UserSecurityConfiguration {
    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "zpantry.security.jwt.enabled", havingValue = "true")
    SecurityFilterChain legacyJwtUserSecurity(HttpSecurity http, LegacyJwtAuthenticationConverter converter)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/Auth/register", "/api/Auth/verify-otp", "/api/Auth/login", "/api/Auth/refresh-token").permitAll()
                        .requestMatchers("/api/Auth/logout", "/api/me/**", "/api/recommendations/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users", "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
        return http.build();
    }

    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "zpantry.security.jwt.enabled", havingValue = "false", matchIfMissing = true)
    SecurityFilterChain failClosedUserSecurity(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/Auth/register", "/api/Auth/verify-otp", "/api/Auth/login", "/api/Auth/refresh-token").permitAll()
                        .requestMatchers("/api/Auth/logout", "/api/me/**", "/api/recommendations/**").denyAll()
                        .requestMatchers("/api/users", "/api/users/**").denyAll()
                        .anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "zpantry.security.jwt.enabled", havingValue = "true")
    JwtDecoder legacyJwtDecoder(
            @Value("${zpantry.security.jwt.secret}") String secret,
            @Value("${zpantry.security.jwt.issuer:}") String issuer,
            @Value("${zpantry.security.jwt.audience:}") String audience,
            TokenRevocationChecker revocationChecker) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) throw new IllegalStateException("Legacy JWT secret must be at least 32 bytes");
        var decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(keyBytes, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();
        var validators = new ArrayList<OAuth2TokenValidator<Jwt>>();
        validators.add(new JwtTimestampValidator(Duration.ZERO));
        if (!issuer.isBlank()) validators.add(new JwtIssuerValidator(issuer));
        if (!audience.isBlank()) validators.add(jwt -> jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null)));
        validators.add(jwt -> {
            String jti = jwt.getId();
            if (jti == null || jti.isBlank()) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Missing token id", null));
            }
            return revocationChecker.isRevoked(jti)
                    ? OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Token revoked", null))
                    : OAuth2TokenValidatorResult.success();
        });
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }
}
