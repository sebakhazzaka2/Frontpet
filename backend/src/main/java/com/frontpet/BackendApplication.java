package com.frontpet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de arranque.
 *
 * <p>Vive en {@code com.frontpet} y no en un sub-paquete a propósito: Spring
 * Boot escanea componentes y entidades desde el paquete de esta clase hacia
 * abajo. Si estuviera en {@code com.frontpet.backend}, los módulos hermanos
 * ({@code catalog}, {@code tenant}, {@code booking}...) quedarían fuera del
 * escaneo — sin repositorios, sin services y, peor, con {@code ddl-auto:
 * validate} pasando en verde por no tener ninguna entidad que revisar.
 *
 * <p>{@link ConfigurationPropertiesScan} habilita clases {@code record}
 * anotadas con {@code @ConfigurationProperties} sin tener que agregar
 * {@code @EnableConfigurationProperties(X.class)} una por una (tarea 1.8,
 * primer uso: {@code LoginRateLimitProperties}).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
