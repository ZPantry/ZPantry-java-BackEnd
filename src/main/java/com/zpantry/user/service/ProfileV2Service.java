package com.zpantry.user.service;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.user.api.ProfileV2Dtos;
import com.zpantry.user.domain.ActivityLevel;
import com.zpantry.user.domain.DietPreference;
import com.zpantry.user.domain.FoodAllergen;
import com.zpantry.user.domain.ProfileGender;
import com.zpantry.user.domain.UserGoal;
import com.zpantry.user.domain.UserProfileEntity;
import com.zpantry.user.persistence.UserProfileRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileV2Service {
    private final UserProfileRepository profiles;
    private final ProfileMetricsCalculator metricsCalculator;

    public ProfileV2Service(UserProfileRepository profiles, ProfileMetricsCalculator metricsCalculator) {
        this.profiles = profiles;
        this.metricsCalculator = metricsCalculator;
    }

    @Transactional(readOnly = true)
    public ApiResponse<ProfileV2Dtos.Response> get(UUID userId) {
        ProfileV2Dtos.Response response = profiles.findByUserIdAndDeletedFalse(userId)
                .filter(profile -> profile.getBirthDate() != null && profile.getActivityLevel() != null)
                .map(this::toResponse)
                .orElse(null);
        return success(response);
    }

    @Transactional
    public ApiResponse<ProfileV2Dtos.Response> update(UUID userId, ProfileV2Dtos.UpdateRequest request) {
        Set<UserGoal> goals = immutableGoals(request.goals());
        Set<FoodAllergen> allergies = normalizedAllergies(request.allergies());
        ProfileV2Dtos.UpdateRequest normalizedRequest = new ProfileV2Dtos.UpdateRequest(
                request.birthDate(), request.gender(), request.heightCm(), request.weightKg(),
                request.activityLevel(), goals, request.dietPreference(), allergies);
        ProfileMetricsCalculator.ProfileMetrics metrics = metricsCalculator.calculate(normalizedRequest, LocalDate.now());

        UserProfileEntity profile = profiles.findByUserIdAndDeletedFalse(userId)
                .orElseGet(() -> new UserProfileEntity(userId));
        profile.updateV2(request.birthDate(), request.gender(), request.heightCm(), request.weightKg(),
                request.activityLevel(), goals, request.dietPreference(), allergies, metrics.bmr(), metrics.tdee(),
                metrics.dailyCalorieTarget(), metrics.dailyProteinTarget());
        return success(toResponse(profiles.save(profile), metrics));
    }

    private ProfileV2Dtos.Response toResponse(UserProfileEntity profile) {
        ProfileV2Dtos.UpdateRequest stored = new ProfileV2Dtos.UpdateRequest(
                profile.getBirthDate(), ProfileGender.valueOf(profile.getGender()), profile.getHeight(), profile.getWeight(),
                ActivityLevel.valueOf(profile.getActivityLevel()), parse(profile.getGoals(), UserGoal.class),
                DietPreference.valueOf(profile.getDietPreference()), parse(profile.getAllergies(), FoodAllergen.class));
        return toResponse(profile, metricsCalculator.calculate(stored, LocalDate.now()));
    }

    private static ProfileV2Dtos.Response toResponse(UserProfileEntity profile, ProfileMetricsCalculator.ProfileMetrics metrics) {
        Set<FoodAllergen> storedAllergies = parse(profile.getAllergies(), FoodAllergen.class);
        Set<FoodAllergen> responseAllergies = storedAllergies.isEmpty()
                ? Set.of(FoodAllergen.NO_ALLERGIES) : storedAllergies;
        return new ProfileV2Dtos.Response(profile.getBirthDate(), ProfileGender.valueOf(profile.getGender()),
                profile.getHeight(), profile.getWeight(), ActivityLevel.valueOf(profile.getActivityLevel()),
                parse(profile.getGoals(), UserGoal.class), DietPreference.valueOf(profile.getDietPreference()),
                responseAllergies, metrics.bmi(), profile.getBmr(), profile.getTdee(),
                profile.getDailyCalorieTarget(), profile.getDailyProteinTarget(), metrics.perMealCalorieTarget(),
                metrics.perMealProteinTarget(), metrics.weightLossAllowed(), metrics.healthWarning());
    }

    private static Set<UserGoal> immutableGoals(Set<UserGoal> values) {
        return values == null || values.isEmpty() ? Set.of() : Set.copyOf(values);
    }

    private static Set<FoodAllergen> normalizedAllergies(Set<FoodAllergen> values) {
        if (values == null || values.isEmpty() || values.contains(FoodAllergen.NO_ALLERGIES) && values.size() == 1) {
            return Set.of();
        }
        if (values.contains(FoodAllergen.NO_ALLERGIES)) {
            throw new IllegalArgumentException("NO_ALLERGIES cannot be combined with another allergen.");
        }
        return Set.copyOf(values);
    }

    private static <T extends Enum<T>> Set<T> parse(String value, Class<T> type) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split(","))
                .map(item -> Enum.valueOf(type, item))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static ApiResponse<ProfileV2Dtos.Response> success(ProfileV2Dtos.Response response) {
        return new ApiResponse<>(true, "", response, null, "", Instant.now());
    }
}
