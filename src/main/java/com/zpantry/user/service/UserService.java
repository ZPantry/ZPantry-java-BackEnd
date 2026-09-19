package com.zpantry.user.service;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.common.api.PagedResponse;
import com.zpantry.user.api.UserResponse;
import com.zpantry.user.api.UserUpdateRequest;
import com.zpantry.user.domain.UserEntity;
import com.zpantry.user.persistence.UserRepository;
import com.zpantry.user.security.AuthenticatedUser;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final LegacyPasswordHasher passwords;

    public UserService(UserRepository users, LegacyPasswordHasher passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> list(int pageIndex, int pageSize) {
        int normalizedPage = Math.max(pageIndex, 1);
        int normalizedSize = Math.clamp(pageSize, 1, 100);
        var pageable = PageRequest.of(normalizedPage - 1, normalizedSize, Sort.by("email").ascending());
        var data = users.findAllByDeletedFalse(pageable).getContent().stream().map(UserService::toResponse).toList();
        int total = Math.toIntExact(users.countByDeletedFalse());
        return PagedResponse.successPage(data, normalizedPage, normalizedSize, total, "", "", Instant.now());
    }

    @Transactional(readOnly = true)
    public ApiResponse<UserResponse> get(UUID id) {
        return users.findByIdAndDeletedFalse(id)
                .map(user -> success(toResponse(user), ""))
                .orElseGet(() -> failure("User not found."));
    }

    @Transactional
    public ApiResponse<UserResponse> update(UUID id, UserUpdateRequest request, AuthenticatedUser identity) {
        if (!identity.userId().equals(id)) throw new OwnerAuthorizationException();
        var user = users.findByIdAndDeletedFalse(id).orElse(null);
        if (user == null) return failure("User not found.");
        String passwordHash = hasNonWhitespace(request.password()) ? passwords.hash(request.password()) : null;
        user.updateProfile(request.fullName(), request.avatarUrl(), passwordHash, Instant.now());
        return success(toResponse(users.save(user)), "User updated successfully.");
    }

    @Transactional
    public ApiResponse<Object> delete(UUID id) {
        var user = users.findByIdAndDeletedFalse(id).orElse(null);
        if (user == null) return new ApiResponse<>(false, "User not found.", null, null, "", Instant.now());
        user.softDelete(Instant.now());
        users.save(user);
        return new ApiResponse<>(true, "User deleted successfully.", null, null, "", Instant.now());
    }

    private static boolean hasNonWhitespace(String value) {
        return value != null && value.codePoints().anyMatch(codePoint ->
                !Character.isWhitespace(codePoint) && !Character.isSpaceChar(codePoint));
    }

    private static UserResponse toResponse(UserEntity user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getAvatarUrl(),
                user.isEmailConfirmed(), user.isActive(), user.getRole(), user.getCreatedAt(), user.getUpdatedAt());
    }

    private static ApiResponse<UserResponse> success(UserResponse data, String message) {
        return new ApiResponse<>(true, message, data, null, "", Instant.now());
    }

    private static ApiResponse<UserResponse> failure(String message) {
        return new ApiResponse<>(false, message, null, null, "", Instant.now());
    }
}
