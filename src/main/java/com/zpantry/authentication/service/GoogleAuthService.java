package com.zpantry.authentication.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleAuthService {

    @Value("${google.client.id}")
    private String clientId;

    public GoogleIdToken.Payload verifyToken(String idTokenString) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                // Chỉ định chính xác Web Client ID của ứng dụng để đối chiếu
                .setAudience(Collections.singletonList(clientId))
                .build();

        // Hàm này sẽ tự động gọi lên Google để check chữ ký, hạn sử dụng...
        GoogleIdToken idToken = verifier.verify(idTokenString);
        
        if (idToken != null) {
            return idToken.getPayload(); // Token chuẩn, trả về thông tin user
        } else {
            throw new Exception("Token không hợp lệ!");
        }
    }
}
