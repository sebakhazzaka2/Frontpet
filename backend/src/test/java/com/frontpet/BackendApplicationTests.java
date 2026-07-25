package com.frontpet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Levanta el contexto completo.
 *
 * <p>Ahora que la clase de arranque está en {@code com.frontpet}, este test
 * sí tiene valor: con {@code ddl-auto: validate}, arrancar implica que
 * Hibernate comparó cada entidad de cada módulo contra el schema real y no
 * encontró diferencias.
 */
@SpringBootTest
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
