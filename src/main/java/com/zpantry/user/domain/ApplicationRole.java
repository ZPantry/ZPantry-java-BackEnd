package com.zpantry.user.domain;

import java.util.Locale;

/** Canonical role values written by the Java application. */
public enum ApplicationRole {
    SUPER_ADMIN, ADMIN, MANAGER, USER;

    public static ApplicationRole fromPersisted(String value) {
        if (value == null) return USER;
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "super_admin" -> SUPER_ADMIN;
            case "admin" -> ADMIN;
            case "manager" -> MANAGER;
            case "user" -> USER;
            default -> throw new IllegalArgumentException("Unsupported persisted role");
        };
    }
}
