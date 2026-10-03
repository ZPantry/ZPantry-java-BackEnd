package com.zpantry.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zpantry.user.api.ProfileV2Dtos;
import com.zpantry.user.domain.ActivityLevel;
import com.zpantry.user.domain.DietPreference;
import com.zpantry.user.domain.ProfileGender;
import com.zpantry.user.domain.UserGoal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProfileMetricsCalculatorTest {
    private final ProfileMetricsCalculator calculator = new ProfileMetricsCalculator();
    private final LocalDate today = LocalDate.of(2026, 10, 3);

    @Test
    void calculatesOtherGenderUsingTheDocumentedNeutralMifflinConstant() {
        var metrics = calculator.calculate(request(LocalDate.of(2000, 10, 3), ProfileGender.OTHER,
                new BigDecimal("170"), new BigDecimal("70"), Set.of()), today);

        assertThat(metrics.bmr()).isEqualByComparingTo("1554.50");
        assertThat(metrics.tdee()).isEqualByComparingTo("2409.48");
        assertThat(metrics.dailyProteinTarget()).isEqualByComparingTo("84.00");
    }

    @Test
    void preventsWeightLossDeficitForAnUnderweightProfile() {
        var metrics = calculator.calculate(request(LocalDate.of(2000, 10, 3), ProfileGender.FEMALE,
                new BigDecimal("170"), new BigDecimal("50"), Set.of(UserGoal.WEIGHT_LOSS)), today);

        assertThat(metrics.bmi()).isLessThan(new BigDecimal("18.5"));
        assertThat(metrics.weightLossAllowed()).isFalse();
        assertThat(metrics.dailyCalorieTarget()).isEqualByComparingTo(metrics.tdee());
        assertThat(metrics.healthWarning()).contains("BMI is below 18.5");
    }

    @Test
    void rejectsBirthDatesOutsideTheSupportedAdultRange() {
        assertThatThrownBy(() -> calculator.calculate(request(LocalDate.of(2020, 1, 1), ProfileGender.MALE,
                new BigDecimal("170"), new BigDecimal("70"), Set.of()), today))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 14 and 120");
    }

    private static ProfileV2Dtos.UpdateRequest request(LocalDate birthDate, ProfileGender gender,
                                                        BigDecimal height, BigDecimal weight, Set<UserGoal> goals) {
        return new ProfileV2Dtos.UpdateRequest(birthDate, gender, height, weight, ActivityLevel.MODERATE,
                goals, DietPreference.DIVERSE, Set.of());
    }
}
