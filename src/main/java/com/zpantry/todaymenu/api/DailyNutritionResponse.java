package com.zpantry.todaymenu.api;
import java.math.BigDecimal; import java.time.LocalDate;
public record DailyNutritionResponse(LocalDate date, BigDecimal targetCalories, BigDecimal consumedCalories, BigDecimal remainingCalories) {}
