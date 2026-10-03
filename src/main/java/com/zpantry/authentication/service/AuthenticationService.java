package com.zpantry.authentication.service;

import com.zpantry.authentication.api.AuthenticationDtos.*;
import com.zpantry.authentication.security.JwtTokenService;
import com.zpantry.authentication.security.DevTokenRevocationService;
import com.zpantry.user.domain.UserEntity;
import com.zpantry.user.persistence.UserRepository;
import com.zpantry.user.service.LegacyPasswordHasher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthenticationService {
    private final UserRepository users; private final LegacyPasswordHasher passwords; private final JwtTokenService tokens;
    private final EmailVerificationPort email;
    private final ObjectProvider<DevTokenRevocationService> revocations; private final SecureRandom random=new SecureRandom();
    public AuthenticationService(UserRepository users,LegacyPasswordHasher passwords,JwtTokenService tokens,EmailVerificationPort email,ObjectProvider<DevTokenRevocationService> revocations){this.users=users;this.passwords=passwords;this.tokens=tokens;this.email=email;this.revocations=revocations;}
    @Transactional public void register(RegisterRequest r){
        if(users.findByEmailAndDeletedFalse(r.email()).isPresent())throw new IllegalStateException("Email already exists.");
        String otp="%06d".formatted(random.nextInt(900000)+100000);
        email.sendVerification(r.email(),r.fullName(),otp);
        users.save(UserEntity.register(r.fullName(),r.email(),passwords.hash(r.password()),otp,Instant.now().plus(5,ChronoUnit.MINUTES)));
    }
    @Transactional public boolean verify(VerifyOtpRequest r){var u=users.findByEmailAndDeletedFalse(r.email()).orElse(null);Instant now=Instant.now();if(u==null||u.isEmailConfirmed()||u.getOtpCode()==null||!u.getOtpCode().equals(r.otpCode())||u.getOtpExpiredAt()==null||u.getOtpExpiredAt().isBefore(now))return false;u.confirmEmail(now);return true;}
    @Transactional public AuthResponse login(LoginRequest r){var u=users.findByEmailAndDeletedFalse(r.email()).orElseThrow(()->new AuthenticationFailure("Invalid email or password."));if(!u.isActive())throw new AuthenticationFailure("Account is inactive.");if(!u.isEmailConfirmed())throw new AuthenticationFailure("Account is not verified yet.");if(!passwords.verify(u.getPasswordHashed(),r.password()))throw new AuthenticationFailure("Invalid email or password.");return issue(u);}
    @Transactional public AuthResponse refresh(RefreshTokenRequest r){if(r.refreshToken()==null||r.refreshToken().isBlank())throw new AuthenticationFailure("Refresh token is required.");var u=users.findByRefreshTokenHashAndDeletedFalse(tokens.hash(r.refreshToken())).orElseThrow(()->new AuthenticationFailure("Invalid refresh token."));if(!u.isActive()||!u.isEmailConfirmed())throw new AuthenticationFailure("Account is not allowed to refresh token.");if(u.getRefreshTokenExpiresAt()==null||!u.getRefreshTokenExpiresAt().isAfter(Instant.now())){u.clearRefreshToken(Instant.now(),false);throw new AuthenticationFailure("Refresh token has expired.");}return issue(u);}
    @Transactional public void logout(String email,String jti,Instant expiresAt){if(email==null||jti==null)throw new AuthenticationFailure("Token is invalid.");users.findByEmailAndDeletedFalse(email).ifPresent(u->u.clearRefreshToken(Instant.now(),true));var service=revocations.getIfAvailable();if(service==null)throw new IllegalStateException("Writable token revocation is unavailable");service.revoke(jti,expiresAt);}
    private AuthResponse issue(UserEntity u){var issued=tokens.issue(u);u.replaceRefreshToken(issued.refreshHash(),issued.refreshExpiry(),Instant.now());return issued.response();}

    @Transactional
    public AuthResponse googleLogin(String email, String name, String pictureUrl) {
        var u = users.findByEmailAndDeletedFalse(email).orElseGet(() -> {
            var newUser = new UserEntity(java.util.UUID.randomUUID(), Instant.now(), name, email,
                    passwords.hash(java.util.UUID.randomUUID().toString()), true, true, "user");
            newUser.updateProfile(name, pictureUrl, null, Instant.now());
            return users.save(newUser);
        });
        if (!u.isActive()) throw new AuthenticationFailure("Account is inactive.");
        if (!u.isEmailConfirmed()) u.confirmEmail(Instant.now());
        return issue(u);
    }
}
