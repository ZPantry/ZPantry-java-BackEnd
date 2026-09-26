package com.zpantry.foundation;

import static org.assertj.core.api.Assertions.assertThat;

import com.zpantry.ZPantryBackendApplication;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@SpringBootTest(classes = ZPantryBackendApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false", "spring.sql.init.mode=never"})
@Import(IsolatedPostgres.class)
class SeedDataIT {
    @Autowired DataSource dataSource;

    @Test
    void syntheticSeedLoadsAfterCleanMigrationWithExpectedRelationships() {
        new ResourceDatabasePopulator(new ClassPathResource("db/seed/test-data.sql")).execute(dataSource);
        var jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE email='admin@test.dev'", String.class)).isEqualTo("admin");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ingredients", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ingredient_aliases a JOIN ingredients i ON i.id=a.ingredient_id", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM recipe_ingredients ri JOIN recipes r ON r.id=ri.recipe_id JOIN ingredients i ON i.id=ri.ingredient_id", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_pantry_items WHERE user_id='00000000-0000-0000-0000-000000000102'", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_pantry_items WHERE user_id='00000000-0000-0000-0000-000000000103'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM today_menu_items WHERE recipe_id='00000000-0000-0000-0000-000000000301'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media_assets WHERE secure_url LIKE 'https://example.invalid/%'", Integer.class)).isEqualTo(1);
    }
}
