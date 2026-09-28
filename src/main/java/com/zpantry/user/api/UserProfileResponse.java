package com.zpantry.user.api;

import com.zpantry.user.domain.DietPreference;
import com.zpantry.user.domain.FoodAllergen;
import com.zpantry.user.domain.UserGoal;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record UserProfileResponse(UUID id, UUID userId, Integer age, String gender,
                                  BigDecimal height, BigDecimal weight, UserGoal goal, DietPreference dietPreference,
                                  Set<FoodAllergen> allergies) {
}
