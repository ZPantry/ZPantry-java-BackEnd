package com.zpantry.common.api;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ResponseContractTest {
    @Test
    void envelopeRetainsNullFieldsAndCamelCaseNames() {
        var mapper = JsonMapper.builder().build();
        var response = new ApiResponse<>(true, "", null, null, "trace-test",
                Instant.parse("2026-09-16T00:00:00Z"));
        var json = mapper.readTree(mapper.writeValueAsString(response));
        assertThat(json.properties()).hasSize(6);
        assertThat(json.get("success").booleanValue()).isTrue();
        assertThat(json.get("data").isNull()).isTrue();
        assertThat(json.get("errors").isNull()).isTrue();
        assertThat(json.get("traceId").asString()).isEqualTo("trace-test");
        assertThat(json.get("timestamp").asString()).isEqualTo("2026-09-16T00:00:00Z");
    }

    @Test
    void paginationPreservesLegacyBoundarySemanticsAndFlatShape() {
        var page = PagedResponse.successPage(List.of("item"), 2, 10, 21, "", "", Instant.EPOCH);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.hasPreviousPage()).isTrue();
        assertThat(page.hasNextPage()).isTrue();
        var empty = PagedResponse.successPage(List.of(), 1, 0, 21, "", "", Instant.EPOCH);
        assertThat(empty.totalPages()).isZero();
        assertThat(empty.hasNextPage()).isFalse();
        var json = JsonMapper.builder().build().valueToTree(page);
        assertThat(json.properties()).hasSize(12);
        assertThat(json.get("pageIndex").intValue()).isEqualTo(2);
        assertThat(json.get("data").isArray()).isTrue();
    }
}
