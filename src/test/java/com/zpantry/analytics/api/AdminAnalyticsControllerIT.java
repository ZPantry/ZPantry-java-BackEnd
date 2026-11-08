package com.zpantry.analytics.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.zpantry.ZPantryBackendApplication;
import com.zpantry.foundation.IsolatedPostgres;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(classes = ZPantryBackendApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false", "spring.flyway.clean-disabled=true",
        "spring.sql.init.mode=never", "spring.jpa.open-in-view=false"})
@Import(IsolatedPostgres.class)
class AdminAnalyticsControllerIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired AdminAnalyticsController analytics;

    @Test
    void overviewUsesLegacyUsersIsDeletedColumnAndExcludesSoftDeletedAccounts() {
        long baseline = jdbc.queryForObject("SELECT count(*) FROM users WHERE is_deleted=false", Long.class);
        insertUser(false, Instant.now());
        insertUser(true, Instant.now());

        var response = analytics.overview(30);

        assertThat(response.success()).isTrue();
        assertThat((Long) response.data().get("totalUsers")).isEqualTo(baseline + 1);
        assertThat((Long) response.data().get("newUsers")).isGreaterThanOrEqualTo(1L);
    }

    private void insertUser(boolean deleted, Instant createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,created_at,is_deleted,full_name,email,password_hashed,is_email_confirmed,is_active,role) VALUES(?,?,?,?,?,?,?,?,?)",
                id, Timestamp.from(createdAt), deleted, "Analytics test", id + "@example.test", "not-a-real-password", true, true, "USER");
    }
}
