package com.zpantry.analytics.api;

import com.zpantry.common.api.ApiResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only aggregates based exclusively on persisted users, payments, and telemetry. */
@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {
    private final JdbcTemplate jdbc;
    public AdminAnalyticsController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(@RequestParam(defaultValue = "30") int days) {
        int safeDays = Math.max(1, Math.min(days, 365));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", scalar("SELECT count(*) FROM users WHERE deleted=false"));
        result.put("newUsers", scalar("SELECT count(*) FROM users WHERE deleted=false AND created_at >= now() - (? * interval '1 day')", safeDays));
        result.put("dau", scalar("SELECT count(DISTINCT user_id) FROM analytics_events WHERE created_at >= date_trunc('day', now())"));
        result.put("mau", scalar("SELECT count(DISTINCT user_id) FROM analytics_events WHERE created_at >= now() - interval '30 day'"));
        result.put("paidRevenueVnd", scalar("SELECT coalesce(sum(amount_vnd),0) FROM payment_transactions WHERE status='PAID'"));
        result.put("transactions", scalar("SELECT count(*) FROM payment_transactions"));
        result.put("periodDays", safeDays);
        return ok(result);
    }

    @GetMapping("/events")
    public ApiResponse<List<Map<String, Object>>> events(@RequestParam(defaultValue = "30") int days) {
        int safeDays = Math.max(1, Math.min(days, 365));
        return ok(rows("SELECT event_name AS name,count(*) AS events,count(DISTINCT user_id) AS users FROM analytics_events WHERE created_at >= now() - (? * interval '1 day') GROUP BY event_name ORDER BY events DESC", safeDays));
    }

    @GetMapping("/screens")
    public ApiResponse<List<Map<String, Object>>> screens(@RequestParam(defaultValue = "30") int days) {
        int safeDays = Math.max(1, Math.min(days, 365));
        return ok(rows("SELECT coalesce(screen_name,'unknown') AS screen,count(*) AS views,count(DISTINCT user_id) AS visitors FROM analytics_events WHERE event_name='SCREEN_VIEW' AND created_at >= now() - (? * interval '1 day') GROUP BY screen_name ORDER BY views DESC", safeDays));
    }

    @GetMapping("/ai-recognition")
    public ApiResponse<List<Map<String, Object>>> recognition() {
        return ok(rows("SELECT feedback,count(*) AS count FROM ai_recognition_feedbacks GROUP BY feedback ORDER BY feedback"));
    }

    @GetMapping("/revenue")
    public ApiResponse<List<Map<String, Object>>> revenue(@RequestParam(defaultValue = "30") int days) {
        int safeDays = Math.max(1, Math.min(days, 365));
        return ok(rows("SELECT to_char(paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh','YYYY-MM-DD') AS day,count(*) AS transactions,coalesce(sum(amount_vnd),0) AS revenue_vnd FROM payment_transactions WHERE status='PAID' AND paid_at >= now() - (? * interval '1 day') GROUP BY day ORDER BY day", safeDays));
    }

    @GetMapping("/transactions")
    public ApiResponse<List<Map<String, Object>>> transactions(@RequestParam(required = false) String status, @RequestParam(defaultValue = "100") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        if (status == null || status.isBlank()) return ok(rows("SELECT id,merchant_order_id,provider,provider_transaction_id,amount_vnd,status,paid_at,created_at,failure_reason FROM payment_transactions ORDER BY created_at DESC LIMIT ?", safeLimit));
        return ok(rows("SELECT id,merchant_order_id,provider,provider_transaction_id,amount_vnd,status,paid_at,created_at,failure_reason FROM payment_transactions WHERE status=? ORDER BY created_at DESC LIMIT ?", status.toUpperCase(), safeLimit));
    }

    @GetMapping(value = "/revenue.csv", produces = "text/csv")
    public ResponseEntity<String> revenueCsv(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (to.isBefore(from) || to.isAfter(from.plusDays(366))) throw new IllegalArgumentException("Invalid export period.");
        StringBuilder csv = new StringBuilder("day,transactions,revenue_vnd\n");
        for (Map<String, Object> row : rows("SELECT to_char(paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh','YYYY-MM-DD') AS day,count(*) AS transactions,coalesce(sum(amount_vnd),0) AS revenue_vnd FROM payment_transactions WHERE status='PAID' AND paid_at >= ? AND paid_at < ? GROUP BY day ORDER BY day", from.atStartOfDay(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(), to.plusDays(1).atStartOfDay(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toInstant())) {
            csv.append(row.get("day")).append(',').append(row.get("transactions")).append(',').append(row.get("revenue_vnd")).append('\n');
        }
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=revenue.csv").contentType(MediaType.valueOf("text/csv;charset=UTF-8")).body(csv.toString());
    }

    private ApiResponse<List<Map<String, Object>>> ok(List<Map<String, Object>> data) { return new ApiResponse<>(true, "", data, null, "", Instant.now()); }
    private ApiResponse<Map<String, Object>> ok(Map<String, Object> data) { return new ApiResponse<>(true, "", data, null, "", Instant.now()); }
    private long scalar(String sql, Object... args) { Long result = jdbc.queryForObject(sql, Long.class, args); return result == null ? 0 : result; }
    private List<Map<String, Object>> rows(String sql, Object... args) { return jdbc.query(sql, (rs, index) -> row(rs), args); }
    private Map<String, Object> row(ResultSet rs) throws SQLException { var metadata = rs.getMetaData(); Map<String, Object> row = new LinkedHashMap<>(); for (int column = 1; column <= metadata.getColumnCount(); column++) row.put(metadata.getColumnLabel(column), rs.getObject(column)); return row; }
}
