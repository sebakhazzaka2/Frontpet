package com.frontpet;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base de todo test de integración: un Postgres real, efímero, exclusivo de
 * esta corrida.
 *
 * <p>Antes los tests corrían contra el Postgres local del dev. Eso trajo un
 * bug real: {@code db/seed-dev/products.sql} (cargado a mano, NO es una
 * migración Flyway) había metido un "Mordedor Kong Clássico" que hacía
 * matchear una búsqueda que el test esperaba con un solo resultado. Acá la DB
 * arranca solo con V1..V9 y nada más, igual en esta máquina que en cualquier
 * otra o en CI.
 *
 * <p>Se arranca a mano en un bloque {@code static} (patrón "singleton
 * container"), <b>NO</b> con {@code @Testcontainers}/{@code @Container}: esa
 * extensión de JUnit para el contenedor al terminar CADA clase de test, así
 * que con 8 clases tendríamos 8 arranques en vez de 1 — y peor, Flyway
 * correría de cero en cada uno, cambiando en silencio la semántica del seed
 * de {@code DataInitializer}. Como acá nunca se llama a {@code stop()}, lo
 * limpia Ryuk (el contenedor centinela que Testcontainers levanta solo)
 * cuando muere la JVM.
 *
 * <p>Un solo contenedor para las ~3 configuraciones de contexto distintas que
 * usan las clases de test ({@code @SpringBootTest} solo,
 * {@code +@AutoConfigureMockMvc}, {@code +@TestPropertySource}): el campo
 * {@code static} se inicializa una vez por classloader, y Surefire corre todo
 * en una sola JVM por defecto ({@code forkCount=1}, {@code reuseForks=true}).
 * Cada contexto corre Flyway, pero contra la misma base — el segundo y el
 * tercero encuentran {@code flyway_schema_history} ya poblada y no migran
 * nada.
 */
public abstract class AbstractIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("frontpet")
                    .withUsername("frontpet")
                    .withPassword("frontpet_dev_pass");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
