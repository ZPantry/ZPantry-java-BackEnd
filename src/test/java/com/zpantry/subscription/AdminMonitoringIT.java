package com.zpantry.subscription;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zpantry.ZPantryBackendApplication;
import com.zpantry.foundation.IsolatedPostgres;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = ZPantryBackendApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false", "spring.sql.init.mode=never",
        "zpantry.security.jwt.enabled=true",
        "zpantry.security.jwt.secret=TEST_ONLY_Admin_monitoring_signing_key_at_least_32_bytes"})
@AutoConfigureMockMvc
@Import(IsolatedPostgres.class)
class AdminMonitoringIT {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void revenueCountsOnlyPaidTransactionsAndUsesBusinessMonth() throws Exception {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zone);
        Instant now = today.atTime(12, 0).atZone(zone).toInstant();
        Instant previousMonth = today.withDayOfMonth(1).minusDays(1).atTime(12, 0).atZone(zone).toInstant();
        insertPayment("PAID", 49_000, now);
        insertPayment("PAID", 12_000, previousMonth);
        insertPayment("PENDING", 99_000, null);
        insertPayment("FAILED", 88_000, null);

        mvc.perform(get("/api/admin/monitoring/revenue").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allTimeVnd").value(61_000))
                .andExpect(jsonPath("$.data.monthVnd").value(49_000))
                .andExpect(jsonPath("$.data.todayVnd").value(49_000))
                .andExpect(jsonPath("$.data.paidTransactions").value(2))
                .andExpect(jsonPath("$.data.recentPayments.length()").value(2));
    }

    @Test
    void monitoringIsAdminOnlyAndIngredientDetailExcludesDeletedRows() throws Exception {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO ingredients(id,created_at,name,normalized_name,unit,calories_per_unit,is_deleted) VALUES (?,?,?,?,?,?,false)",
                id, Timestamp.from(Instant.now()), "Admin test ingredient", "admin test ingredient", "g", 30);

        mvc.perform(get("/api/admin/monitoring/revenue")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/monitoring/services").with(user())).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/monitoring/services").with(admin())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("DATABASE"))
                .andExpect(jsonPath("$.data[0].available").value(true));
        mvc.perform(get("/api/ingredients/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Admin test ingredient"));
        jdbc.update("UPDATE ingredients SET is_deleted=true WHERE id=?", id);
        mvc.perform(get("/api/ingredients/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    private void insertPayment(String status, long amount, Instant paidAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO payment_transactions(id,created_at,user_id,provider,merchant_order_id,amount_vnd,status,paid_at)
                VALUES (?,?,?,?,?,?,?,?)
                """, id, Timestamp.from(Instant.now()), UUID.randomUUID(), "PAYOS", "ZP-" + id,
                amount, status, paidAt == null ? null : Timestamp.from(paidAt));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor admin() {
        return jwt().jwt(token -> token.claim("role", "admin").claim("jti", "admin-monitoring-test"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor user() {
        return jwt().jwt(token -> token.claim("role", "user").claim("jti", "user-monitoring-test"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
