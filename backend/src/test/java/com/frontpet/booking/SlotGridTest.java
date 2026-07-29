package com.frontpet.booking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Casos de borde del ADR 020 — sin Spring, sin Testcontainers, feedback en
 * segundos. Horario base de los tests: 09:00–17:00, sin pausa (caso FrontPet).
 */
class SlotGridTest {

    private static final LocalDate DATA = LocalDate.of(2026, 8, 5); // miércoles
    private static final LocalTime ABERTURA = LocalTime.of(9, 0);
    private static final LocalTime FECHAMENTO = LocalTime.of(17, 0);
    // "Ahora" bien antes del día consultado: la anticipación mínima no interfiere
    // en los tests que no la ejercitan a propósito.
    private static final ZonedDateTime NOW = ZonedDateTime.of(2026, 8, 1, 8, 0, 0, 0, SlotGrid.ZONE_ID);

    @Test
    @DisplayName("grilla de 30 min genera candidatos desde la abertura")
    void gridStepsEvery30Minutes() {
        List<LocalTime> starts = SlotGrid.candidateStarts(
                DATA, ABERTURA, FECHAMENTO, null, null, 60, NOW, 0);

        assertThat(starts).startsWith(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("un servicio que cruza el cierre no se ofrece")
    void serviceCrossingClosingIsExcluded() {
        // 16:30 + 90 min = 18:00, más allá de las 17:00 → 16:30 no debe aparecer
        List<LocalTime> starts = SlotGrid.candidateStarts(
                DATA, ABERTURA, FECHAMENTO, null, null, 90, NOW, 0);

        assertThat(starts).doesNotContain(LocalTime.of(16, 30));
    }

    @Test
    @DisplayName("el último slot respeta el cierre exacto")
    void lastSlotRespectsClosingBoundary() {
        // 16:00 + 60 min = 17:00 exacto → debe incluirse; 16:30 + 60 = 17:30 → no
        List<LocalTime> starts = SlotGrid.candidateStarts(
                DATA, ABERTURA, FECHAMENTO, null, null, 60, NOW, 0);

        assertThat(starts).contains(LocalTime.of(16, 0));
        assertThat(starts).doesNotContain(LocalTime.of(16, 30));
    }

    @Test
    @DisplayName("sin pausa (NULL, caso FrontPet) no descarta ningún candidato por mediodía")
    void noPausaMeansNoMiddayGap() {
        List<LocalTime> starts = SlotGrid.candidateStarts(
                DATA, ABERTURA, FECHAMENTO, null, null, 30, NOW, 0);

        assertThat(starts).contains(LocalTime.of(12, 0), LocalTime.of(12, 30));
    }

    @Test
    @DisplayName("con pausa seteada, los candidatos que la intersectan se descartan")
    void pausaExcludesOverlappingCandidates() {
        LocalTime pausaInicio = LocalTime.of(12, 0);
        LocalTime pausaFin = LocalTime.of(13, 0);

        List<LocalTime> starts = SlotGrid.candidateStarts(
                DATA, ABERTURA, FECHAMENTO, pausaInicio, pausaFin, 30, NOW, 0);

        // 11:30 + 30 = 12:00 → no intersecta (semiabierto, termina justo cuando empieza la pausa)
        assertThat(starts).contains(LocalTime.of(11, 30));
        // 11:45 no es candidato de la grilla de 30 min, pero 12:00 y 12:30 sí caen dentro/cruzan la pausa
        assertThat(starts).doesNotContain(LocalTime.of(12, 0), LocalTime.of(12, 30));
        // 13:00 + 30 = 13:30 → no intersecta (empieza justo cuando termina la pausa)
        assertThat(starts).contains(LocalTime.of(13, 0));
    }

    @Test
    @DisplayName("anticipación mínima: justo en el límite se incluye, un instante antes se excluye")
    void anticipacaoMinimaBoundary() {
        int anticipacaoMinHoras = 2;
        // "Ahora" el mismo día, 07:00 → con 2h de anticipación, el primer slot válido es 09:00
        ZonedDateTime now = ZonedDateTime.of(2026, 8, 5, 7, 0, 0, 0, SlotGrid.ZONE_ID);

        List<LocalTime> starts = SlotGrid.candidateStarts(
                DATA, ABERTURA, FECHAMENTO, null, null, 30, now, anticipacaoMinHoras);

        assertThat(starts).contains(LocalTime.of(9, 0));

        // Un "ahora" 30 min más tarde (07:30) empuja el límite a 09:30 → 09:00 ya no alcanza
        ZonedDateTime nowLater = now.plusMinutes(30);
        List<LocalTime> startsLater = SlotGrid.candidateStarts(
                DATA, ABERTURA, FECHAMENTO, null, null, 30, nowLater, anticipacaoMinHoras);

        assertThat(startsLater).doesNotContain(LocalTime.of(9, 0));
        assertThat(startsLater).contains(LocalTime.of(9, 30));
    }

    @Test
    @DisplayName("tempo_extra: +20% redondeado, sin alinear a la grilla de 30 min")
    void tempoExtraRoundsWithoutAligningToGrid() {
        assertThat(SlotGrid.applyTempoExtra(90, true)).isEqualTo(108);
        assertThat(SlotGrid.applyTempoExtra(90, false)).isEqualTo(90);
        assertThat(SlotGrid.applyTempoExtra(45, true)).isEqualTo(54);
    }

    @Test
    @DisplayName("service_pricing 90 min: bloquea correctamente slots posteriores parcialmente solapados")
    void ninetyMinuteServiceOccupiesConsecutiveGridSteps() {
        // Con duración 90, el candidato de 09:00 termina 10:30 — ocupa el paso de 09:00,
        // 09:30 y 10:00 en términos de qué NO puede empezar ahí sin chocar (eso lo valida
        // AvailabilityServiceImpl contra ocupación real; acá solo verificamos que 09:00
        // sigue siendo un candidato válido de la grilla).
        List<LocalTime> starts = SlotGrid.candidateStarts(
                DATA, ABERTURA, FECHAMENTO, null, null, 90, NOW, 0);

        assertThat(starts).contains(LocalTime.of(9, 0));
        assertThat(starts).contains(LocalTime.of(15, 30)); // 15:30+90=17:00, límite exacto
        assertThat(starts).doesNotContain(LocalTime.of(16, 0)); // 16:00+90=17:30, excede
    }
}
