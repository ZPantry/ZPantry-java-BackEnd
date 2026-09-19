package com.zpantry.authentication.service;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ResendEmailVerificationAdapter implements EmailVerificationPort {
    private final RestClient client; private final String key; private final String from;
    public ResendEmailVerificationAdapter(@Value("${zpantry.email.resend.api-key:}") String key,
            @Value("${zpantry.email.from:}") String from) {
        this.client=RestClient.builder().baseUrl("https://api.resend.com").build();this.key=key;this.from=from;
    }
    @Override public void sendVerification(String email,String fullName,String otp) {
        if(key.isBlank()||from.isBlank()) throw new IllegalStateException("Email delivery is not configured");
        client.post().uri("/emails").header("Authorization","Bearer "+key).body(Map.of(
                "from",from,"to",List.of(email),"subject","ZPantry OTP Verification",
                "html","Hello "+(fullName==null?"":fullName)+", your OTP is <b>"+otp+"</b>"))
                .retrieve().toBodilessEntity();
    }
}
