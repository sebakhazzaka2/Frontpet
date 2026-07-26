package com.frontpet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de arranque.
 *
 * <p>Vive en {@code com.frontpet} y no en un sub-paquete a propósito: Spring
 * Boot escanea componentes y entidades desde el paquete de esta clase hacia
 * abajo. Si estuviera en {@code com.frontpet.backend}, los módulos hermanos
 * ({@code catalog}, {@code tenant}, {@code booking}...) quedarían fuera del
 * escaneo — sin repositorios, sin services y, peor, con {@code ddl-auto:
 * validate} pasando en verde por no tener ninguna entidad que revisar.
 */
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
