package com.zpantry.integration.ai;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class HttpAiServiceHealthProbe implements AiServiceHealthProbe {
    private final String healthUrl;

    HttpAiServiceHealthProbe(@Value("${zpantry.ai.service-url:http://localhost:8000}") String serviceUrl) {
        String baseUrl = serviceUrl.endsWith("/") ? serviceUrl.substring(0, serviceUrl.length() - 1) : serviceUrl;
        this.healthUrl = baseUrl + "/ai/health";
    }

    @Override
    public void probe() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(healthUrl).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(5_000);
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("AI health endpoint responded with HTTP " + status);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("AI health endpoint is unavailable", exception);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
