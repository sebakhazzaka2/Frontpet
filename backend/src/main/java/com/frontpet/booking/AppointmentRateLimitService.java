package com.frontpet.booking;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Contador de requests a {@code POST /api/v1/appointments} por IP, en memoria
 * (tarea 5.8). Tercera copia literal de {@code OrderRateLimitService} — ver
 * su javadoc para el porqué de no unificar en {@code common/} todavía
 * (CLAUDE.md §6: recién acá se cumplen los 3 casos de uso reales, y el propio
 * plan de sprint difiere esa unificación a Sprint 7 para no tocar 3 módulos
 * dentro del sprint más riesgoso).
 */
@Service
public class AppointmentRateLimitService {

    static final int MAX_TRACKED_KEYS = 10_000;

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    private final AppointmentRateLimitProperties props;

    public AppointmentRateLimitService(Clock clock, AppointmentRateLimitProperties props) {
        this.clock = clock;
        this.props = props;
    }

    private record Attempt(int count, Instant windowStartedAt, Instant blockedUntil) {

        boolean isBlocked(Instant now) {
            return blockedUntil != null && now.isBefore(blockedUntil);
        }

        boolean windowExpired(Instant now, Duration window) {
            return now.isAfter(windowStartedAt.plus(window));
        }
    }

    /** @return segundos que faltan para poder reintentar, o 0 si no hay bloqueo. */
    public long blockedSecondsRemaining(String key) {
        if (!props.enabled()) {
            return 0;
        }
        Attempt attempt = attempts.get(key);
        Instant now = clock.instant();
        if (attempt == null || !attempt.isBlocked(now)) {
            return 0;
        }
        return Duration.between(now, attempt.blockedUntil()).toSeconds() + 1;
    }

    /** Registra una request (exitosa o no) — no hay noción de "éxito" que resetee el contador. */
    public void recordRequest(String key) {
        if (!props.enabled()) {
            return;
        }
        sweepIfNeeded();
        Instant now = clock.instant();
        attempts.compute(key, (k, prev) -> {
            if (prev == null || prev.windowExpired(now, props.window())) {
                return new Attempt(1, now, null);
            }
            int count = prev.count() + 1;
            Instant blockedUntil = count >= props.maxRequests()
                    ? now.plus(props.window())
                    : null;
            return new Attempt(count, prev.windowStartedAt(), blockedUntil);
        });
    }

    /** Solo para tests: el bean es singleton y Spring cachea el ApplicationContext entre clases. */
    void clear() {
        attempts.clear();
    }

    /** Solo para tests: verificar que el barrido perezoso acota el mapa. */
    int trackedKeyCount() {
        return attempts.size();
    }

    private void sweepIfNeeded() {
        if (attempts.size() <= MAX_TRACKED_KEYS) {
            return;
        }
        Instant now = clock.instant();
        attempts.values().removeIf(a -> !a.isBlocked(now) && a.windowExpired(now, props.window()));
    }
}
