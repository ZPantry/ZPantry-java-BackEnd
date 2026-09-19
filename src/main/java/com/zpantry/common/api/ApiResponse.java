package com.zpantry.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** Source-derived envelope; HTTP status selection belongs to each legacy adapter. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(boolean success, String message, T data,
        List<ApiErrorDetail> errors, String traceId, Instant timestamp) {}
