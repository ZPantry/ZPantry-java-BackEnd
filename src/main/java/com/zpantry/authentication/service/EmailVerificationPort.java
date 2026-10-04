package com.zpantry.authentication.service;

public interface EmailVerificationPort {
    void sendVerification(String email, String fullName, String otp);

    default void sendPasswordReset(String email, String fullName, String otp) {
        sendVerification(email, fullName, otp);
    }
}
