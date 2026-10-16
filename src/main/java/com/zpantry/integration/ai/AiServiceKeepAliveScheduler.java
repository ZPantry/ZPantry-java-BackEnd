package com.zpantry.integration.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the AI web service active while this backend is active. A separate external monitor must
 * still call this backend's public /health endpoint, because a sleeping backend cannot schedule work.
 */
@Component
@ConditionalOnProperty(name = "zpantry.keepalive.enabled", havingValue = "true")
class AiServiceKeepAliveScheduler {
    private static final Logger log = LoggerFactory.getLogger(AiServiceKeepAliveScheduler.class);
    private final AiServiceHealthProbe probe;

    AiServiceKeepAliveScheduler(AiServiceHealthProbe probe) {
        this.probe = probe;
    }

    @Scheduled(fixedDelayString = "${zpantry.keepalive.interval-ms:600000}", initialDelayString = "${zpantry.keepalive.initial-delay-ms:60000}")
    void pingAiService() {
        try {
            probe.probe();
            log.info("AI keepalive succeeded");
        } catch (RuntimeException exception) {
            log.warn("AI keepalive failed: {}", exception.getMessage());
        }
    }
}
