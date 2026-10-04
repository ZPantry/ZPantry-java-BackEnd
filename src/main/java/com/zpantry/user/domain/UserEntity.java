package com.zpantry.user.domain;

import com.zpantry.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {
    @Column(name = "full_name", length = 150)
    private String fullName;
    @Column(name = "email", nullable = false, length = 200)
    private String email;
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    @Column(name = "password_hashed", nullable = false, length = 500)
    private String passwordHashed;
    @Column(name = "otp_code", length = 6)
    private String otpCode;
    @Column(name = "otp_expired_at")
    private Instant otpExpiredAt;
    @Column(name = "otp_retry_count", nullable = false)
    private int otpRetryCount;
    @Column(name = "is_email_confirmed", nullable = false)
    private boolean emailConfirmed;
    @Column(name = "is_active", nullable = false)
    private boolean active;
    @Column(name = "role", nullable = false, length = 50)
    private String role;
    @Column(name = "refresh_token_hash", length = 128)
    private String refreshTokenHash;
    @Column(name = "refresh_token_expires_at")
    private Instant refreshTokenExpiresAt;

    protected UserEntity() {}

    public UserEntity(UUID id, Instant createdAt, String fullName, String email,
            String passwordHashed, boolean emailConfirmed, boolean active, String role) {
        this.id = id;
        this.createdAt = createdAt;
        this.fullName = fullName;
        this.email = email;
        this.passwordHashed = passwordHashed;
        this.emailConfirmed = emailConfirmed;
        this.active = active;
        this.role = role;
    }

    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getPasswordHashed() { return passwordHashed; }
    public String getOtpCode() { return otpCode; }
    public Instant getOtpExpiredAt() { return otpExpiredAt; }
    public int getOtpRetryCount() { return otpRetryCount; }
    public boolean isEmailConfirmed() { return emailConfirmed; }
    public boolean isActive() { return active; }
    public String getRole() { return role; }
    public String getRefreshTokenHash() { return refreshTokenHash; }
    public Instant getRefreshTokenExpiresAt() { return refreshTokenExpiresAt; }

    public void updateProfile(String fullName, String avatarUrl, String passwordHash, Instant now) {
        if (fullName != null) this.fullName = fullName;
        if (avatarUrl != null) this.avatarUrl = avatarUrl;
        if (passwordHash != null) this.passwordHashed = passwordHash;
        this.updatedAt = now;
    }

    public void softDelete(Instant now) {
        this.deleted = true;
        this.deletedAt = now;
    }

    public ApplicationRole applicationRole() { return ApplicationRole.fromPersisted(role); }

    public void changeRole(ApplicationRole newRole, Instant now) {
        this.role = newRole.name();
        this.updatedAt = now;
    }

    public void changeActive(boolean newActive, Instant now) {
        this.active = newActive;
        this.updatedAt = now;
    }

    public static UserEntity register(String fullName, String email, String passwordHash,
            String otpCode, Instant otpExpiresAt) {
        UserEntity user = new UserEntity(UUID.randomUUID(), Instant.now(), fullName, email,
                passwordHash, false, true, "user");
        user.otpCode = otpCode;
        user.otpExpiredAt = otpExpiresAt;
        return user;
    }

    public static UserEntity bootstrapSuperAdmin(String email, String passwordHash) {
        return new UserEntity(UUID.randomUUID(), Instant.now(), "Initial Super Admin", email,
                passwordHash, true, true, ApplicationRole.SUPER_ADMIN.name());
    }

    public void confirmEmail(Instant now) {
        emailConfirmed = true; otpCode = null; otpExpiredAt = null; updatedAt = now;
    }

    public void issueOtp(String otpCode, Instant otpExpiresAt, Instant now) {
        this.otpCode = otpCode;
        this.otpExpiredAt = otpExpiresAt;
        this.otpRetryCount = 0;
        this.updatedAt = now;
    }

    public void resetPassword(String passwordHash, Instant now) {
        this.passwordHashed = passwordHash;
        this.otpCode = null;
        this.otpExpiredAt = null;
        this.otpRetryCount = 0;
        this.refreshTokenHash = null;
        this.refreshTokenExpiresAt = null;
        this.updatedAt = now;
    }

    public void recordResetOtpFailure(Instant now) {
        otpRetryCount++;
        if (otpRetryCount >= 5) {
            otpCode = null;
            otpExpiredAt = null;
        }
        updatedAt = now;
    }

    public void replaceRefreshToken(String hash, Instant expiresAt, Instant now) {
        refreshTokenHash = hash; refreshTokenExpiresAt = expiresAt; updatedAt = now;
    }

    public void clearRefreshToken(Instant now, boolean updateAudit) {
        refreshTokenHash = null; refreshTokenExpiresAt = null;
        if (updateAudit) updatedAt = now;
    }
}
