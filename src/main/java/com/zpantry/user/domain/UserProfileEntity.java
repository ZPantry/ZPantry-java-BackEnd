package com.zpantry.user.domain;

import com.zpantry.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfileEntity extends BaseEntity {
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "age")
    private Integer age;

    @Column(name = "gender", length = 50)
    private String gender;

    @Column(name = "height", precision = 5, scale = 2)
    private BigDecimal height;

    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "goal", length = 200)
    private String goal;

    @Column(name = "diet_preference", length = 500)
    private String dietPreference;

    @Column(name = "allergies", columnDefinition = "text")
    private String allergies;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "activity_level", length = 32)
    private String activityLevel;

    @Column(name = "goals", columnDefinition = "text")
    private String goals;

    @Column(name = "bmr", precision = 10, scale = 2)
    private BigDecimal bmr;

    @Column(name = "tdee", precision = 10, scale = 2)
    private BigDecimal tdee;

    @Column(name = "daily_calorie_target", precision = 10, scale = 2)
    private BigDecimal dailyCalorieTarget;

    @Column(name = "daily_protein_target", precision = 10, scale = 2)
    private BigDecimal dailyProteinTarget;

    protected UserProfileEntity() {}

    public UserProfileEntity(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() { return userId; }
    public Integer getAge() { return age; }
    public String getGender() { return gender; }
    public BigDecimal getHeight() { return height; }
    public BigDecimal getWeight() { return weight; }
    public String getGoal() { return goal; }
    public String getDietPreference() { return dietPreference; }
    public String getAllergies() { return allergies; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getActivityLevel() { return activityLevel; }
    public String getGoals() { return goals; }
    public BigDecimal getBmr() { return bmr; }
    public BigDecimal getTdee() { return tdee; }
    public BigDecimal getDailyCalorieTarget() { return dailyCalorieTarget; }
    public BigDecimal getDailyProteinTarget() { return dailyProteinTarget; }

    public void update(Integer age, String gender, BigDecimal height, BigDecimal weight,
                       UserGoal goal, DietPreference dietPreference, java.util.Set<FoodAllergen> allergies) {
        this.age = age;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.goal = goal == null ? null : goal.name();
        this.dietPreference = dietPreference == null ? null : dietPreference.name();
        this.allergies = allergies == null ? null : allergies.stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(","));
        this.touch();
    }

    public void updateV2(LocalDate birthDate, ProfileGender gender, BigDecimal heightCm,
                         BigDecimal weightKg, ActivityLevel activityLevel,
                         java.util.Set<UserGoal> goals, DietPreference dietPreference,
                         java.util.Set<FoodAllergen> allergies, BigDecimal bmr,
                         BigDecimal tdee, BigDecimal dailyCalorieTarget,
                         BigDecimal dailyProteinTarget) {
        this.birthDate = birthDate;
        this.gender = gender.name();
        this.height = heightCm;
        this.weight = weightKg;
        this.activityLevel = activityLevel.name();
        this.goals = serialize(goals);
        this.dietPreference = dietPreference.name();
        this.allergies = serialize(allergies);
        this.bmr = bmr;
        this.tdee = tdee;
        this.dailyCalorieTarget = dailyCalorieTarget;
        this.dailyProteinTarget = dailyProteinTarget;
        this.touch();
    }

    private static <T extends Enum<T>> String serialize(java.util.Set<T> values) {
        return values == null || values.isEmpty() ? null
                : values.stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(","));
    }
}
