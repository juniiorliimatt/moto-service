package br.com.moto;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base dos ITs: sobe o contexto Spring inteiro (Liquibase rodando todas as migrations +
 * Hibernate {@code ddl-auto=validate}) contra um Postgres real e descartável, com o mesmo
 * role/schema restrito de produção ({@code moto_service}/{@code moto}, ver
 * {@code testcontainers-init.sql}) — nunca o superusuário do container, nem o Postgres de dev
 * compartilhado. O container é estático: compartilhado entre as subclasses da mesma JVM de
 * teste, e os ITs isolam os dados usando donos (owner_username) distintos por teste.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
public abstract class PostgresIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:18"))
            .withDatabaseName("workbox")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("testcontainers-init.sql");

    @DynamicPropertySource
    static void datasourceProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://%s:%d/workbox"
                .formatted(POSTGRES.getHost(), POSTGRES.getMappedPort(5432)));
        registry.add("spring.datasource.username", () -> "moto_service");
        registry.add("spring.datasource.password", () -> "moto_service");
    }
}
