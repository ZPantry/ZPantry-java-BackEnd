package com.zpantry.user.api;

public record UserUpdateRequest(String fullName, String avatarUrl, String password) {}
