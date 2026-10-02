package com.zpantry.user.api;

import com.zpantry.user.domain.DietPreference;
import com.zpantry.user.domain.FoodAllergen;
import com.zpantry.user.domain.UserGoal;

import java.math.BigDecimal;
import java.util.Set;

public record UserProfileUpdateRequest(Integer age, String gender, BigDecimal height,
                                       BigDecimal weight, UserGoal goal, DietPreference dietPreference,
                                       Set<FoodAllergen> allergies) {
}
