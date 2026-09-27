package com.zpantry.user.api;

import java.math.BigDecimal;
import java.util.UUID;

public record UserProfileResponse(UUID id, UUID userId, Integer age, String gender, 
        BigDecimal height, BigDecimal weight, String goal, String dietPreference, String allergies) {}
