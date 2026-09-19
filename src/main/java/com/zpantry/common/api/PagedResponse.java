package com.zpantry.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** Flat envelope matching the legacy inherited JSON shape, not Spring Page. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record PagedResponse<T>(boolean success, String message, List<T> data,
        List<ApiErrorDetail> errors, String traceId, Instant timestamp,
        int pageIndex, int pageSize, int totalItems, int totalPages,
        boolean hasNextPage, boolean hasPreviousPage) {

    public static <T> PagedResponse<T> successPage(List<T> items, int pageIndex,
            int pageSize, int totalItems, String message, String traceId, Instant timestamp) {
        int totalPages = pageSize <= 0 ? 0 : (int) Math.ceil(totalItems / (double) pageSize);
        return new PagedResponse<>(true, message, items, null, traceId, timestamp,
                pageIndex, pageSize, totalItems, totalPages,
                pageIndex < totalPages, pageIndex > 1);
    }
}
