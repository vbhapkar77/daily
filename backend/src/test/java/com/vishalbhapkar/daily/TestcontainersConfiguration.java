package com.vishalbhapkar.daily;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Provides a Testcontainers-managed Postgres for integration tests. Spring Boot 3.1+'s
 * {@code @ServiceConnection} auto-wires this container as the application's DataSource —
 * Flyway runs against it, JPA talks to it, the entire request chain works end-to-end.
 *
 * <p>Image version is pinned to match production (Neon runs Postgres 16) and our
 * local Docker Compose stack. Per ADR-0007.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    }
}
