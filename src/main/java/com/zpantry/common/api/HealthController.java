package com.zpantry.common.api;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lightweight public liveness endpoint. It deliberately performs no database or provider calls. */
@RestController
public class HealthController {
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return new ApiResponse<>(true, "ok", Map.of("status", "healthy"), null, "", Instant.now());
    }
}
