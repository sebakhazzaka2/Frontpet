package com.frontpet.identity;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test puro (sin Spring), como {@code R2StorageServiceImplTest}: un
 * {@link ConcurrentHashMap} con lógica de ventana no necesita contexto.
 * Maneja el tiempo con un {@link MutableClock} propio en vez de
 * {@code Thread.sleep} real.
 */
class LoginAttemptServiceTest {

    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_FAILURES = 3;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-07-28T12:00:00Z"));
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService(clock, new LoginRateLimitProperties(true, MAX_FAILURES, WINDOW));
    }

    @Test
    @DisplayName("no bloquea hasta llegar a maxFailures")
    void doesNotBlockBeforeMaxFailures() {
        for (int i = 0; i < MAX_FAILURES - 1; i++) {
            service.recordFailure("ip:203.0.113.10");
        }

        assertThat(service.blockedSecondsRemaining("ip:203.0.113.10")).isZero();
    }

    @Test
    @DisplayName("el fallo número maxFailures bloquea con un retryAfter positivo")
    void blocksOnReachingMaxFailures() {
        for (int i = 0; i < MAX_FAILURES; i++) {
            service.recordFailure("ip:203.0.113.10");
        }

        assertThat(service.blockedSecondsRemaining("ip:203.0.113.10")).isPositive();
    }

    @Test
    @DisplayName("reset() después de un login exitoso libera el bloqueo")
    void resetClearsBlock() {
        for (int i = 0; i < MAX_FAILURES; i++) {
            service.recordFailure("ip:203.0.113.10");
        }
        assertThat(service.blockedSecondsRemaining("ip:203.0.113.10")).isPositive();

        service.reset("ip:203.0.113.10");

        assertThat(service.blockedSecondsRemaining("ip:203.0.113.10")).isZero();
    }

    @Test
    @DisplayName("la ventana vencida reinicia el contador en vez de acumular")
    void expiredWindowRestartsCounter() {
        service.recordFailure("ip:203.0.113.10");
        service.recordFailure("ip:203.0.113.10");
        clock.advance(WINDOW.plusMinutes(1));

        // Si el contador no se hubiera reiniciado, este sería el 3er fallo
        // acumulado y bloquearía. Como la ventana venció, es el 1ro de una
        // ventana nueva.
        service.recordFailure("ip:203.0.113.10");

        assertThat(service.blockedSecondsRemaining("ip:203.0.113.10")).isZero();
    }

    @Test
    @DisplayName("con enabled=false no bloquea nunca, aunque se superen los fallos")
    void disabledNeverBlocks() {
        LoginAttemptService disabled =
                new LoginAttemptService(clock, new LoginRateLimitProperties(false, MAX_FAILURES, WINDOW));

        for (int i = 0; i < MAX_FAILURES + 5; i++) {
            disabled.recordFailure("ip:203.0.113.10");
        }

        assertThat(disabled.blockedSecondsRemaining("ip:203.0.113.10")).isZero();
    }

    @Test
    @DisplayName("el barrido perezoso mantiene el mapa acotado al superar el tope de claves")
    void sweepBoundsTrackedKeys() {
        for (int i = 0; i < LoginAttemptService.MAX_TRACKED_KEYS + 1_000; i++) {
            service.recordFailure("ip:203.0.113." + i);
            clock.advance(Duration.ofMillis(1));
        }
        // Todas las claves anteriores a esta ventana ya vencieron, así que el
        // próximo recordFailure que cruce el tope dispara el barrido.
        clock.advance(WINDOW.plusMinutes(1));
        service.recordFailure("ip:203.0.113.999999");

        assertThat(service.trackedKeyCount()).isLessThanOrEqualTo(LoginAttemptService.MAX_TRACKED_KEYS);
    }

    /** {@link Clock} mutable para avanzar el tiempo sin {@code Thread.sleep}. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
