package com.zpantry.pantryimport.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

class ImageAnalysisQuotaServiceTest {
    @Test
    void rejectsRequestWhenAtomicQuotaUpdateFindsNoRemainingAllowance() {
        var jdbc = org.mockito.Mockito.mock(JdbcTemplate.class);
        var service = new ImageAnalysisQuotaService(jdbc, 3, ZoneId.of("Asia/Ho_Chi_Minh"),
                Clock.fixed(Instant.parse("2026-10-04T00:00:00Z"), ZoneId.of("UTC")));
        when(jdbc.queryForObject(any(String.class), eq(Integer.class), org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertThatThrownBy(() -> service.consume(UUID.randomUUID()))
                .isInstanceOf(ImageAnalysisQuotaExceededException.class);
    }
}
