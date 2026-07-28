package com.frontpet.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link Clock} inyectable en vez de {@code Instant.now()} directo. Primer
 * consumidor: {@code LoginAttemptService} (tarea 1.8) — sin esto, cada test
 * de ventana/expiración necesitaría {@code Thread.sleep} real.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
