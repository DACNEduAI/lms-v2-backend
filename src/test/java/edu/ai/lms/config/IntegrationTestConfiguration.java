package edu.ai.lms.config;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import io.restassured.RestAssured;

/**
 * Base configuration for integration tests using TestContainers.
 * This class should be extended by integration test classes to share common container setup.
 * Uses a singleton container pattern for better performance across test classes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("dev")
public abstract class IntegrationTestConfiguration {

    @LocalServerPort
    protected Integer port;

    // Singleton container - started once and reused across all tests
    protected static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:18-alpine")
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass");
        postgres.start();


    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Application name (required by OpenApiConfig)
        registry.add("spring.application.name", () -> "lms_platform_test");
        
        // Database configuration
        registry.add("spring.datasource.url", () -> {
            String jdbcUrl = postgres.getJdbcUrl();
            String separator = jdbcUrl.contains("?") ? "&" : "?";
            return jdbcUrl + separator + "currentSchema=edu";
        });
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // JDBC schema initialization
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:schema.sql");
        // Disable data.sql for tests to ensure clean database state
        registry.add("spring.sql.init.data-locations", () -> "");
        
        // Disable Liquibase for tests
        registry.add("spring.liquibase.enabled", () -> "false");
        
        // JWT configuration
        registry.add("jwt.secret", () -> "testsecretkeytestsecretkeytestsecretkeytestsecretkeytestsecretkey");
        registry.add("jwt.expiration", () -> "3600000");
        
        // AI Service configuration
        registry.add("ai.service.url", () -> "http://localhost:8000");
        
        // Disable Spring Boot Admin client for tests
        registry.add("spring.boot.admin.client.enabled", () -> "false");

        // Quartz jobs are validated separately and should not auto-start in existing IT suites.
        registry.add("spring.quartz.auto-startup", () -> "false");

        // Disable Spring Modulith JDBC event publication for tests
        registry.add("spring.modulith.events.jdbc.enabled", () -> "false");
    }

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.baseURI = "http://localhost:" + port;
    }
}
