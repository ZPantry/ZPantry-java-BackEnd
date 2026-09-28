package com.zpantry.user.api;

import java.math.BigDecimal;

public record UserProfileUpdateRequest(Integer age, String gender, BigDecimal height,
        BigDecimal weight, String goal, String dietPreference, String allergies) {}
