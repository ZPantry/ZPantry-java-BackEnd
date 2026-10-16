package com.zpantry.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HealthControllerTest {
    @Test
    void returnsAHealthyPublicLivenessResponse() {
        var response = new HealthController().health();

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("ok");
        assertThat(response.data()).containsEntry("status", "healthy");
        assertThat(response.timestamp()).isNotNull();
    }
}
