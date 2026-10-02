package com.zpantry.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zpantry.ZPantryBackendApplication;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(classes = ZPantryBackendApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false", "spring.flyway.clean-disabled=true",
        "spring.sql.init.mode=never", "spring.jpa.open-in-view=false"})
@Import(IsolatedPostgres.class)
class FoundationPersistenceIT {
    @Autowired DataSource dataSource;
    @Autowired PostgreSQLContainer postgres;
    @Autowired Flyway flyway;

    @Test
    void flywayCreatesCleanContainerAndApplicationValidatesOnJava21() throws Exception {
        assertThat(Runtime.version().feature()).isEqualTo(21);
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).isEqualTo(postgres.getJdbcUrl());
            assertThat(connection.getMetaData().getDatabaseMajorVersion()).isEqualTo(16);
            assertThat(connection.getCatalog()).isEqualTo("zpantry_foundation_test");
        }
        var jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject("SELECT extversion FROM pg_extension WHERE extname='vector'", String.class)).isEqualTo("0.8.2");
        assertThat(jdbc.queryForObject("SELECT to_regclass('flyway_schema_history')", String.class)).isEqualTo("flyway_schema_history");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class)).isEqualTo(5);
    }

    @Test
    void catalogContainsAllLegacyTablesAndCriticalTypes() {
        var jdbc = new JdbcTemplate(dataSource);
        var expected = java.util.Set.of("users", "ingredients", "ingredient_aliases", "recipes", "recipe_ingredients",
                "user_pantry_items", "meal_recommendations", "meal_recommendation_items", "recommendation_feedbacks",
                "media_assets", "today_menu_items", "cooking_logs", "pantry_usage_logs");
        var actual = new java.util.HashSet<>(jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'", String.class));
        assertThat(actual).containsAll(expected);
        assertThat(jdbc.queryForObject("SELECT format_type(a.atttypid,a.atttypmod) FROM pg_attribute a WHERE a.attrelid='ingredients'::regclass AND a.attname='embedding'", String.class)).isEqualTo("vector(1536)");
        assertThat(jdbc.queryForObject("SELECT format_type(a.atttypid,a.atttypmod) FROM pg_attribute a WHERE a.attrelid='recipes'::regclass AND a.attname='embedding'", String.class)).isEqualTo("vector(1536)");
        assertThat(jdbc.queryForObject("SELECT data_type FROM information_schema.columns WHERE table_name='users' AND column_name='id'", String.class)).isEqualTo("uuid");
        assertThat(jdbc.queryForObject("SELECT data_type FROM information_schema.columns WHERE table_name='users' AND column_name='created_at'", String.class)).isEqualTo("timestamp with time zone");
    }

    @Test
    void secondFlywayMigrationHasNoPendingChanges() {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    @Transactional
    void uuidNumericTimestampAndVectorRoundTripThroughMigratedSchema() {
        var jdbc = new JdbcTemplate(dataSource);
        var id = UUID.randomUUID();
        var instant = Instant.parse("2026-09-16T02:03:04.123456Z");
        String vector = "[" + String.join(",", Collections.nCopies(1536, "0.25")) + "]";
        jdbc.update("INSERT INTO ingredients(id,created_at,name,normalized_name,calories_per_unit,embedding) VALUES (?,?,?,?,?,?::vector)", id, java.sql.Timestamp.from(instant), "Foundation probe", "foundation probe", new BigDecimal("12.3456"), vector);
        assertThat(jdbc.queryForObject("SELECT id FROM ingredients WHERE id=?", UUID.class, id)).isEqualTo(id);
        assertThat(jdbc.queryForObject("SELECT calories_per_unit FROM ingredients WHERE id=?", BigDecimal.class, id)).isEqualByComparingTo("12.3456");
        assertThat(jdbc.queryForObject("SELECT created_at FROM ingredients WHERE id=?", java.time.OffsetDateTime.class, id).toInstant()).isEqualTo(instant);
        assertThat(jdbc.queryForObject("SELECT vector_dims(embedding) FROM ingredients WHERE id=?", Integer.class, id)).isEqualTo(1536);
        assertThatThrownBy(() -> jdbc.update("UPDATE ingredients SET embedding='[1,2,3]'::vector WHERE id=?", id))
                .isInstanceOf(org.springframework.dao.DataAccessException.class).hasMessageContaining("1536");
    }
}
