package com.zpantry.foundation;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

@TestConfiguration(proxyBeanMethods = false)
public class IsolatedPostgres {
    @Bean(initMethod = "start", destroyMethod = "stop")
    public PostgreSQLContainer postgres() {
        // Manifest digest for pgvector/pgvector:0.8.2-pg16, verified 2026-09-16.
        return new PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector@sha256:00ba258a66dac104fd5171074a0084462a64a1369d8513f3d0a634e2f24d15bc")
                .asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("zpantry_foundation_test")
                .withUsername("foundation_test")
                .withPassword("container-only")
                .withReuse(false);
    }

    // Constructed exclusively from the started container, never from environment DB settings.
    @Bean
    public DataSource dataSource(PostgreSQLContainer postgres) {
        return new DriverManagerDataSource(postgres.getJdbcUrl(),
                postgres.getUsername(), postgres.getPassword());
    }
}
