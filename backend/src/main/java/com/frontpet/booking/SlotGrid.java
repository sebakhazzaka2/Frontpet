package com.frontpet.booking;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera los horarios de inicio candidatos de un día, dados el horario de
 * atención, la pausa (opcional) y la duración total del turno — sin mirar
 * ocupación, eso es responsabilidad de {@code AvailabilityServiceImpl}
 * (ADR 020, pasos 6-7).
 *
 * <p>Sin Spring a propósito: acá vive el 70% de los bugs posibles del sprint
 * (ADR 020), y se testea entero en segundos sin Testcontainers.
 *
 * <p>Nunca {@link ZoneId#systemDefault()}: la JVM del contenedor corre en
 * UTC, y un default silenciosamente correcto en dev rompería en producción.
 */
public final class SlotGrid {

    public static final int GRID_MINUTES = 30;
    public static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

    private SlotGrid() {
        // clase de utilidad, no se instancia
    }

    /**
     * Candidatos de inicio dentro de {@code [abertura, fechamento]}, en pasos
     * de {@link #GRID_MINUTES}, que (a) terminan antes o justo en
     * {@code fechamento}, (b) no intersectan la pausa, y (c) respetan la
     * anticipación mínima contra {@code now}.
     *
     * @param pausaInicio          NULL = sin pausa (caso FrontPet)
     * @param duracaoTotalMinutes  ya con el +20% de {@code tempo_extra} aplicado
     *                             si corresponde (ver {@link #applyTempoExtra})
     * @param now                  instante de referencia para la anticipación mínima
     */
    public static List<LocalTime> candidateStarts(
            LocalDate data,
            LocalTime abertura,
            LocalTime fechamento,
            LocalTime pausaInicio,
            LocalTime pausaFin,
            int duracaoTotalMinutes,
            ZonedDateTime now,
            int anticipacaoMinHoras
    ) {
        List<LocalTime> starts = new ArrayList<>();
        int aberturaMin = toMinutes(abertura);
        int fechamentoMin = toMinutes(fechamento);

        for (int startMin = aberturaMin; startMin + duracaoTotalMinutes <= fechamentoMin; startMin += GRID_MINUTES) {
            int endMin = startMin + duracaoTotalMinutes;
            if (intersectsPausa(startMin, endMin, pausaInicio, pausaFin)) {
                continue;
            }
            LocalTime start = LocalTime.of(startMin / 60, startMin % 60);
            if (!respectsAnticipacaoMinima(data, start, now, anticipacaoMinHoras)) {
                continue;
            }
            starts.add(start);
        }
        return starts;
    }

    /** Rango semiabierto — mismo criterio que el solapamiento de turnos (ADR 020 §3). */
    static boolean intersectsPausa(int startMin, int endMin, LocalTime pausaInicio, LocalTime pausaFin) {
        if (pausaInicio == null || pausaFin == null) {
            return false;
        }
        int pausaInicioMin = toMinutes(pausaInicio);
        int pausaFinMin = toMinutes(pausaFin);
        return startMin < pausaFinMin && endMin > pausaInicioMin;
    }

    static boolean respectsAnticipacaoMinima(LocalDate data, LocalTime start, ZonedDateTime now, int anticipacaoMinHoras) {
        ZonedDateTime candidateStart = ZonedDateTime.of(data, start, ZONE_ID);
        return !candidateStart.isBefore(now.plusHours(anticipacaoMinHoras));
    }

    /**
     * {@code tempo_extra} (+20%, ADR 011) afecta solo duración, nunca precio.
     * El resultado <b>no se alinea</b> a la grilla de 30 min (ADR 020 §2): la
     * grilla gobierna los inicios ofrecidos, no los fines — redondear el fin
     * a 30 regalaría minutos de agenda que la capacidad real no tiene.
     */
    public static int applyTempoExtra(int duracaoBaseMinutes, boolean tempoExtra) {
        return tempoExtra ? Math.round(duracaoBaseMinutes * 1.2f) : duracaoBaseMinutes;
    }

    private static int toMinutes(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }
}
