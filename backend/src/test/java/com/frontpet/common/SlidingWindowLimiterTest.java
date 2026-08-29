package com.frontpet.common;

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
 * Unit test puro (sin Spring) del núcleo compartido por los 3 rate limits del
 * sistema (login, orders, appointments — ver {@code docs/pending-decisions.md}
 * §9). Antes de la unificación esta lógica se probaba solo desde
 * {@code identity.LoginAttemptServiceTest}; ahora vive acá porque es donde
 * vive el código. Maneja el tiempo con un {@link MutableClock} propio en vez
 * de {@code Thread.sleep} real.
 */
class SlidingWindowLimiterTest {

    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_ATTEMPTS = 3;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-07-28T12:00:00Z"));
    private SlidingWindowLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new SlidingWindowLimiter(clock, true, MAX_ATTEMPTS, WINDOW);
    }

    @Test
    @DisplayName("no bloquea hasta llegar a maxAttempts")
    void doesNotBlockBeforeMaxAttempts() {
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            limiter.recordAttempt("ip:203.0.113.10");
        }

        assertThat(limiter.blockedSecondsRemaining("ip:203.0.113.10")).isZero();
    }

    @Test
    @DisplayName("el intento número maxAttempts bloquea con un retryAfter positivo")
    void blocksOnReachingMaxAttempts() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            limiter.recordAttempt("ip:203.0.113.10");
        }

        assertThat(limiter.blockedSecondsRemaining("ip:203.0.113.10")).isPositive();
    }

    @Test
    @DisplayName("reset() libera el bloqueo")
    void resetClearsBlock() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            limiter.recordAttempt("ip:203.0.113.10");
        }
        assertThat(limiter.blockedSecondsRemaining("ip:203.0.113.10")).isPositive();

        limiter.reset("ip:203.0.113.10");

        assertThat(limiter.blockedSecondsRemaining("ip:203.0.113.10")).isZero();
    }

    @Test
    @DisplayName("otra clave no comparte el contador")
    void differentKeyHasItsOwnBudget() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            limiter.recordAttempt("ip:203.0.113.10");
        }

        assertThat(limiter.blockedSecondsRemaining("ip:203.0.113.11")).isZero();
    }

    @Test
    @DisplayName("la ventana vencida reinicia el contador en vez de acumular")
    void expiredWindowRestartsCounter() {
        limiter.recordAttempt("ip:203.0.113.10");
        limiter.recordAttempt("ip:203.0.113.10");
        clock.advance(WINDOW.plusMinutes(1));

        // Si el contador no se hubiera reiniciado, este sería el 3er intento
        // acumulado y bloquearía. Como la ventana venció, es el 1ro de una
        // ventana nueva.
        limiter.recordAttempt("ip:203.0.113.10");

        assertThat(limiter.blockedSecondsRemaining("ip:203.0.113.10")).isZero();
    }

    @Test
    @DisplayName("con enabled=false no bloquea nunca, aunque se superen los intentos")
    void disabledNeverBlocks() {
        SlidingWindowLimiter disabled = new SlidingWindowLimiter(clock, false, MAX_ATTEMPTS, WINDOW);

        for (int i = 0; i < MAX_ATTEMPTS + 5; i++) {
            disabled.recordAttempt("ip:203.0.113.10");
        }

        assertThat(disabled.blockedSecondsRemaining("ip:203.0.113.10")).isZero();
    }

    @Test
    @DisplayName("el barrido perezoso mantiene el mapa acotado al superar el tope de claves")
    void sweepBoundsTrackedKeys() {
        for (int i = 0; i < SlidingWindowLimiter.MAX_TRACKED_KEYS + 1_000; i++) {
            limiter.recordAttempt("ip:203.0.113." + i);
            clock.advance(Duration.ofMillis(1));
        }
        // Todas las claves anteriores a esta ventana ya vencieron, así que el
        // próximo recordAttempt que cruce el tope dispara el barrido.
        clock.advance(WINDOW.plusMinutes(1));
        limiter.recordAttempt("ip:203.0.113.999999");

        assertThat(limiter.trackedKeyCount()).isLessThanOrEqualTo(SlidingWindowLimiter.MAX_TRACKED_KEYS);
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
