package com.zpantry.subscription.service;

import com.zpantry.integration.ai.AiServiceHealthProbe;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMonitoringService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final JdbcTemplate jdbc;
    private final AiServiceHealthProbe aiProbe;
    private final Clock clock;

    @Autowired
    public AdminMonitoringService(JdbcTemplate jdbc, AiServiceHealthProbe aiProbe) {
        this(jdbc, aiProbe, Clock.system(BUSINESS_ZONE));
    }

    AdminMonitoringService(JdbcTemplate jdbc, AiServiceHealthProbe aiProbe, Clock clock) {
        this.jdbc = jdbc;
        this.aiProbe = aiProbe;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RevenueReport revenue() {
        LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        BigDecimal allTime = sumPaid(null, null);
        BigDecimal thisMonth = sumPaid(today.withDayOfMonth(1), today.withDayOfMonth(1).plusMonths(1));
        BigDecimal todayTotal = sumPaid(today, today.plusDays(1));
        long paidCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment_transactions WHERE status='PAID'", Long.class);
        List<PaidTransaction> latest = jdbc.query("""
                SELECT id, merchant_order_id, amount_vnd, paid_at
                FROM payment_transactions WHERE status='PAID' AND paid_at IS NOT NULL
                ORDER BY paid_at DESC, id DESC LIMIT 10
                """, (row, number) -> new PaidTransaction(
                row.getObject("id", UUID.class), row.getString("merchant_order_id"),
                row.getBigDecimal("amount_vnd"), row.getTimestamp("paid_at").toInstant()));
        return new RevenueReport(allTime, thisMonth, todayTotal, paidCount, latest);
    }

    public List<ServiceStatus> services() {
        return List.of(check("DATABASE", () -> jdbc.queryForObject("SELECT 1", Integer.class)),
                check("AI_SERVICE", aiProbe::probe));
    }

    private ServiceStatus check(String name, Runnable probe) {
        long start = System.nanoTime();
        try {
            probe.run();
            return new ServiceStatus(name, true, (System.nanoTime() - start) / 1_000_000, Instant.now(clock));
        } catch (RuntimeException unavailable) {
            return new ServiceStatus(name, false, (System.nanoTime() - start) / 1_000_000, Instant.now(clock));
        }
    }

    private BigDecimal sumPaid(LocalDate start, LocalDate end) {
        if (start == null) {
            return jdbc.queryForObject("SELECT COALESCE(SUM(amount_vnd), 0) FROM payment_transactions WHERE status='PAID'", BigDecimal.class);
        }
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(amount_vnd), 0) FROM payment_transactions
                WHERE status='PAID' AND paid_at >= ? AND paid_at < ?
                """, BigDecimal.class, Timestamp.from(start.atStartOfDay(BUSINESS_ZONE).toInstant()),
                Timestamp.from(end.atStartOfDay(BUSINESS_ZONE).toInstant()));
    }

    public record ServiceStatus(String name, boolean available, long latencyMs, Instant checkedAt) { }
    public record PaidTransaction(UUID id, String orderId, BigDecimal amountVnd, Instant paidAt) { }
    public record RevenueReport(BigDecimal allTimeVnd, BigDecimal monthVnd, BigDecimal todayVnd,
            long paidTransactions, List<PaidTransaction> recentPayments) { }
}
