package com.zpantry.user.api;

import com.zpantry.user.domain.ActivityLevel;
import com.zpantry.user.domain.DietPreference;
import com.zpantry.user.domain.FoodAllergen;
import com.zpantry.user.domain.ProfileGender;
import com.zpantry.user.domain.UserGoal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public final class ProfileV2Dtos {
    private ProfileV2Dtos() {}

    public record UpdateRequest(
            @NotNull @Past LocalDate birthDate,
            @NotNull ProfileGender gender,
            @NotNull @DecimalMin("50") @DecimalMax("300") BigDecimal heightCm,
            @NotNull @DecimalMin("20") @DecimalMax("500") BigDecimal weightKg,
            @NotNull ActivityLevel activityLevel,
            Set<UserGoal> goals,
            @NotNull DietPreference dietPreference,
            Set<FoodAllergen> allergies) {}

    public record Response(
            LocalDate birthDate,
            ProfileGender gender,
            BigDecimal heightCm,
            BigDecimal weightKg,
            ActivityLevel activityLevel,
            Set<UserGoal> goals,
            DietPreference dietPreference,
            Set<FoodAllergen> allergies,
            BigDecimal bmi,
            BigDecimal bmr,
            BigDecimal tdee,
            BigDecimal dailyCalorieTarget,
            BigDecimal dailyProteinTarget,
            BigDecimal perMealCalorieTarget,
            BigDecimal perMealProteinTarget,
            boolean weightLossAllowed,
            String healthWarning) {}
}
