package com.zpantry.user.service;

import com.zpantry.user.api.ProfileV2Dtos;
import com.zpantry.user.domain.ActivityLevel;
import com.zpantry.user.domain.ProfileGender;
import com.zpantry.user.domain.UserGoal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Deterministic, explainable nutrition metrics used by profile and recommendation V2. */
@Component
public class ProfileMetricsCalculator {
    private static final BigDecimal UNDERWEIGHT_BMI = new BigDecimal("18.5");

    public ProfileMetrics calculate(ProfileV2Dtos.UpdateRequest request, LocalDate today) {
        int age = Period.between(request.birthDate(), today).getYears();
        if (age < 14 || age > 120) {
            throw new IllegalArgumentException("Birth date must represent an age between 14 and 120.");
        }

        BigDecimal heightMeters = request.heightCm().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal bmi = request.weightKg().divide(heightMeters.multiply(heightMeters), 2, RoundingMode.HALF_UP);
        BigDecimal bmr = request.weightKg().multiply(new BigDecimal("10"))
                .add(request.heightCm().multiply(new BigDecimal("6.25")))
                .subtract(BigDecimal.valueOf(age).multiply(new BigDecimal("5")))
                .add(genderConstant(request.gender()));
        bmr = scale(bmr);

        BigDecimal tdee = scale(bmr.multiply(activityMultiplier(request.activityLevel())));
        boolean wantsWeightLoss = request.goals() != null && request.goals().contains(UserGoal.WEIGHT_LOSS);
        boolean weightLossAllowed = bmi.compareTo(UNDERWEIGHT_BMI) >= 0;
        BigDecimal calories = wantsWeightLoss && weightLossAllowed
                ? tdee.multiply(new BigDecimal("0.85")).max(bmr)
                : tdee;
        BigDecimal proteinPerKg = containsProteinGoal(request.goals())
                ? new BigDecimal("1.60") : new BigDecimal("1.20");
        BigDecimal protein = request.weightKg().multiply(proteinPerKg);

        return new ProfileMetrics(bmi, bmr, tdee, scale(calories), scale(protein),
                scale(calories.divide(new BigDecimal("3"), 4, RoundingMode.HALF_UP)),
                scale(protein.divide(new BigDecimal("3"), 4, RoundingMode.HALF_UP)),
                weightLossAllowed,
                wantsWeightLoss && !weightLossAllowed
                        ? "Weight-loss targets are disabled because BMI is below 18.5." : null);
    }

    private static boolean containsProteinGoal(Set<UserGoal> goals) {
        return goals != null && (goals.contains(UserGoal.HIGH_PROTEIN) || goals.contains(UserGoal.MUSCLE_GAIN));
    }

    private static BigDecimal genderConstant(ProfileGender gender) {
        return switch (gender) {
            case MALE -> new BigDecimal("5");
            case FEMALE -> new BigDecimal("-161");
            // A documented neutral constant avoids silently treating OTHER as male or female.
            case OTHER -> new BigDecimal("-78");
        };
    }

    private static BigDecimal activityMultiplier(ActivityLevel activityLevel) {
        return switch (activityLevel) {
            case SEDENTARY -> new BigDecimal("1.20");
            case LIGHT -> new BigDecimal("1.375");
            case MODERATE -> new BigDecimal("1.55");
            case HIGH -> new BigDecimal("1.725");
        };
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public record ProfileMetrics(BigDecimal bmi, BigDecimal bmr, BigDecimal tdee,
                                 BigDecimal dailyCalorieTarget, BigDecimal dailyProteinTarget,
                                 BigDecimal perMealCalorieTarget, BigDecimal perMealProteinTarget,
                                 boolean weightLossAllowed, String healthWarning) {}
}
