package com.zpantry.authentication.security;

import com.zpantry.authentication.api.AuthenticationDtos.AuthResponse;
import com.zpantry.user.domain.UserEntity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class JwtTokenService {
    private final byte[] secret; private final String issuer; private final String audience;
    private final long accessMinutes; private final long refreshDays; private final ObjectMapper json;
    private final SecureRandom random = new SecureRandom();
    public JwtTokenService(@Value("${zpantry.security.jwt.secret:}") String secret,
            @Value("${zpantry.security.jwt.issuer:}") String issuer,
            @Value("${zpantry.security.jwt.audience:}") String audience,
            @Value("${zpantry.security.jwt.access-token-minutes:60}") long accessMinutes,
            @Value("${zpantry.security.jwt.refresh-token-days:7}") long refreshDays) {
        this.secret=secret.getBytes(StandardCharsets.UTF_8); this.issuer=issuer; this.audience=audience;
        this.accessMinutes=accessMinutes; this.refreshDays=refreshDays; this.json=new ObjectMapper();
        if (this.secret.length>0 && this.secret.length<32) throw new IllegalStateException("Legacy JWT secret must be at least 32 bytes");
    }
    public IssuedTokens issue(UserEntity user) {
        if (secret.length<32) throw new IllegalStateException("JWT issuance is not configured");
        Instant now=Instant.now(), expiry=now.plus(accessMinutes,ChronoUnit.MINUTES);
        String role=user.getRole()==null||user.getRole().isBlank()?"user":user.getRole().trim().toLowerCase();
        var claims=new java.util.LinkedHashMap<String,Object>();
        claims.put("sub",user.getEmail()); claims.put("email",user.getEmail()); claims.put("jti",UUID.randomUUID().toString().replace("-",""));
        claims.put("userId",user.getId().toString()); claims.put("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier",user.getId().toString());
        claims.put("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name",user.getFullName()==null?"":user.getFullName());
        claims.put("fullName",user.getFullName()==null?"":user.getFullName()); claims.put("isEmailConfirmed",Boolean.toString(user.isEmailConfirmed()));
        claims.put("http://schemas.microsoft.com/ws/2008/06/identity/claims/role",role); claims.put("role",role);
        claims.put("nbf",now.getEpochSecond()); claims.put("exp",expiry.getEpochSecond());
        if(!issuer.isBlank())claims.put("iss",issuer); if(!audience.isBlank())claims.put("aud",audience);
        String access=encode(claims); byte[] bytes=new byte[64]; random.nextBytes(bytes);
        String refresh=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new IssuedTokens(new AuthResponse(access,expiry,user.getFullName()==null?"":user.getFullName(),user.getEmail(),refresh,role),
                hash(refresh), now.plus(refreshDays,ChronoUnit.DAYS));
    }
    public String hash(String token) { try{return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);} }
    private String encode(Object claims) { try { String h=Base64.getUrlEncoder().withoutPadding().encodeToString(json.writeValueAsBytes(java.util.Map.of("alg","HS256","typ","JWT"))); String p=Base64.getUrlEncoder().withoutPadding().encodeToString(json.writeValueAsBytes(claims)); Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(secret,"HmacSHA256"));return h+"."+p+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(m.doFinal((h+"."+p).getBytes(StandardCharsets.US_ASCII))); } catch(Exception e){throw new IllegalStateException(e);} }
    public record IssuedTokens(AuthResponse response,String refreshHash,Instant refreshExpiry){}
}
