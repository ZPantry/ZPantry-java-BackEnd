package com.zpantry.user.service;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.user.api.UserProfileResponse;
import com.zpantry.user.api.UserProfileUpdateRequest;
import com.zpantry.user.domain.UserProfileEntity;
import com.zpantry.user.persistence.UserProfileRepository;
import com.zpantry.user.persistence.UserRepository;
import com.zpantry.user.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;

    public UserProfileService(UserProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ApiResponse<UserProfileResponse> get(UUID userId, AuthenticatedUser identity) {
        if (!identity.userId().equals(userId) || !identity.roles().contains("user")) {
            throw new OwnerAuthorizationException();
        }

        return profileRepository.findByUserIdAndDeletedFalse(userId)
                .map(this::mapToResponse)
                .map(UserProfileService::success)
                .orElseGet(() -> success(new UserProfileResponse(null, userId, null, null, null, null, null, null, null)));
    }

    @Transactional
    public ApiResponse<UserProfileResponse> update(UUID userId, UserProfileUpdateRequest request, AuthenticatedUser identity) {
        if (!identity.userId().equals(userId) || !identity.roles().contains("user")) {
            throw new OwnerAuthorizationException();
        }

        if (userRepository.findByIdAndDeletedFalse(userId).isEmpty()) {
            return failure("User not found");
        }

        UserProfileEntity profile = profileRepository.findByUserIdAndDeletedFalse(userId)
                .orElseGet(() -> new UserProfileEntity(userId));

        profile.update(request.age(), request.gender(), request.height(), request.weight(),
                request.goal(), request.dietPreference(), request.allergies());

        profile = profileRepository.save(profile);
        return success(mapToResponse(profile));
    }

    private UserProfileResponse mapToResponse(UserProfileEntity entity) {
        return new UserProfileResponse(entity.getId(), entity.getUserId(), entity.getAge(),
                entity.getGender(), entity.getHeight(), entity.getWeight(),
                entity.getGoal(), entity.getDietPreference(), entity.getAllergies());
    }

    private static ApiResponse<UserProfileResponse> success(UserProfileResponse data) {
        return new ApiResponse<>(true, "", data, null, "", Instant.now());
    }

    private static ApiResponse<UserProfileResponse> failure(String message) {
        return new ApiResponse<>(false, message, null, null, "", Instant.now());
    }
}
