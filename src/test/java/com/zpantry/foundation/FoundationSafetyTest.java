package com.zpantry.foundation;

import org.junit.jupiter.api.Test;
import java.util.Properties;
import static org.assertj.core.api.Assertions.assertThat;

class FoundationSafetyTest {
    private record ValidationProbe(@jakarta.validation.constraints.NotBlank String value) {}

    @Test
    void beanValidationProviderIsAvailable() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new ValidationProbe(""))).hasSize(1);
            assertThat(validator.validate(new ValidationProbe("present"))).isEmpty();
        }
    }

    @Test
    void defaultConfigurationCannotInitializeOrMigrateSchema() throws Exception {
        var properties = new Properties();
        try (var stream = getClass().getResourceAsStream("/application.properties")) {
            properties.load(stream);
        }
        assertThat(Runtime.version().feature()).isEqualTo(21);
        assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(properties.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(properties.getProperty("spring.flyway.enabled")).isEqualTo("false");
        assertThat(properties.getProperty("spring.flyway.baseline-on-migrate")).isEqualTo("false");
        assertThat(properties.getProperty("spring.flyway.clean-disabled")).isEqualTo("true");
        assertThat(properties).doesNotContainKey("spring.datasource.url");
    }
}
