package com.zpantry.authentication.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("dev")
@Component
public class DevResendEmailVerficationAdapter implements EmailVerificationPort {
    @Override
    public void sendVerification(String email, String fullName, String otp) {
        System.out.println("Sending verification email... to "+fullName +" with email "+email+"\n");
        System.out.println("your otp demo: "+otp);
    }
}
