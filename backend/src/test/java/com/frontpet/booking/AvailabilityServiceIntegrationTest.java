package com.frontpet.booking;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.booking.domain.Appointment;
import com.frontpet.booking.domain.AppointmentRepository;
import com.frontpet.booking.domain.AppointmentStatus;
import com.frontpet.booking.domain.BusinessHours;
import com.frontpet.booking.domain.BusinessHoursRepository;
import com.frontpet.booking.domain.Porte;
import com.frontpet.booking.domain.ScheduleBlock;
import com.frontpet.booking.domain.ScheduleBlockRepository;
import com.frontpet.booking.domain.ServiceOffering;
import com.frontpet.booking.domain.ServiceOfferingRepository;
import com.frontpet.booking.domain.ServicePricing;
import com.frontpet.booking.domain.ServicePricingRepository;
import com.frontpet.booking.domain.ServiceType;
import com.frontpet.booking.dto.AvailabilityQuery;
import com.frontpet.booking.dto.AvailabilityResponse;
import com.frontpet.booking.dto.ComboPricing;
import com.frontpet.booking.dto.Indisponibilidade;
import com.frontpet.booking.dto.SlotDto;
import com.frontpet.common.UuidV7;
import com.frontpet.tenant.domain.Tenant;
import com.frontpet.tenant.domain.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Casos de borde del cálculo de disponibilidad (ADR 005 / ADR 020), issue #47.
 *
 * <p>Tenant propio por test, no el sembrado por V5: ese ya trae
 * {@code business_hours} de los 7 días y chocaría con
 * {@code uq_business_hours_tenant_dia}.
 *
 * <p>Fechas relativas a hoy en vez de un {@code Clock} fijo: la ventana de
 * anticipación se ejercita contra el reloj real, que es lo que corre en
 * producción. {@link #proximaQuarta()} da un miércoles con margen suficiente
 * para no rozar ni la anticipación mínima (1 h) ni la máxima (60 días).
 */
@SpringBootTest
@Transactional
class AvailabilityServiceIntegrationTest extends AbstractIntegrationTest {

    private static final int CAPACIDADE = 2;

    @Autowired AvailabilityService availabilityService;
    @Autowired TenantRepository tenantRepository;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;
    @Autowired ServicePricingRepository servicePricingRepository;
    @Autowired BusinessHoursRepository businessHoursRepository;
    @Autowired ScheduleBlockRepository scheduleBlockRepository;
    @Autowired AppointmentRepository appointmentRepository;

    private UUID tenantId;
    private ServiceOffering banhoBase;
    private ServiceOffering adicional;

    @BeforeEach
    void seed() {
        tenantId = novoTenant(CAPACIDADE);

        // Seg-Sex 09:00-17:00, Sáb 09:00-19:00, Dom fechado — mismo patrón que V5.
        for (int dia = 1; dia <= 5; dia++) {
            businessHours(tenantId, (short) dia, true, LocalTime.of(9, 0), LocalTime.of(17, 0));
        }
        businessHours(tenantId, (short) 6, true, LocalTime.of(9, 0), LocalTime.of(19, 0));
        businessHours(tenantId, (short) 7, false, LocalTime.of(9, 0), LocalTime.of(17, 0));

        banhoBase = servico(tenantId, ServiceType.BASE, "Banho Teste");
        pricing(banhoBase, Porte.M, "59.00", 60);
        pricing(banhoBase, Porte.G, "79.00", 90);

        adicional = servico(tenantId, ServiceType.ADDON, "Tosa Teste");
        pricing(adicional, Porte.M, "20.00", 20);
        pricing(adicional, Porte.G, "25.00", 30);
    }

    // ---- combo: duración y precio ---------------------------------------

    @Test
    @DisplayName("la duración total es base + Σ adicionais para ese porte")
    void durationIsBasePlusAddons() {
        AvailabilityResponse soloBase = consultar(Porte.M, List.of(), proximaQuarta());
        assertThat(soloBase.duracaoTotalMinutes()).isEqualTo(60);
        assertThat(soloBase.precoTotal()).isEqualByComparingTo("59.00");

        AvailabilityResponse comAdicional = consultar(Porte.M, List.of(adicional.getId()), proximaQuarta());
        assertThat(comAdicional.duracaoTotalMinutes()).isEqualTo(80);
        assertThat(comAdicional.precoTotal()).isEqualByComparingTo("79.00");
    }

    @Test
    @DisplayName("cambiar el porte cambia la duración y, con ella, los slots ofrecidos")
    void changingPorteChangesOfferedSlots() {
        LocalDate quarta = proximaQuarta();

        List<String> slotsM = horarios(consultar(Porte.M, List.of(), quarta));   // 60 min
        List<String> slotsG = horarios(consultar(Porte.G, List.of(), quarta));   // 90 min

        // Cierre 17:00: con 60 min el último inicio es 16:00; con 90 min, 15:30.
        assertThat(slotsM).contains("16:00");
        assertThat(slotsG).doesNotContain("16:00").contains("15:30");
    }

    @Test
    @DisplayName("un servicio que no es BASE como base, o uno que no es ADDON como adicional, es 400")
    void rejectsWrongServiceTypes() {
        assertThatThrownBy(() -> availabilityService.resolveCombo(
                tenantId, adicional.getId(), List.of(), Porte.M))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não é um banho base");

        assertThatThrownBy(() -> availabilityService.resolveCombo(
                tenantId, banhoBase.getId(), List.of(banhoBase.getId() + 9999), Porte.M))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("un porte sin tarifa cargada es 400, no una duración de cero")
    void rejectsPorteWithoutPricing() {
        // Solo se cargaron tarifas M y G para el banho base.
        assertThatThrownBy(() -> availabilityService.resolveCombo(
                tenantId, banhoBase.getId(), List.of(), Porte.GG))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sem tarifa");
    }

    @Test
    @DisplayName("el mismo adicional repetido se cobra una sola vez")
    void duplicateAddonCountsOnce() {
        Long addonId = adicional.getId();
        ComboPricing combo = availabilityService.resolveCombo(
                tenantId, banhoBase.getId(), List.of(addonId, addonId), Porte.M);

        assertThat(combo.addons()).hasSize(1);
        assertThat(combo.totalDurationMinutes()).isEqualTo(80);
    }

    // ---- motivos de indisponibilidad ------------------------------------

    @Test
    @DisplayName("domingo devuelve DIA_INATIVO con la grilla vacía")
    void sundayIsInactive() {
        LocalDate domingo = proximaQuarta();
        while (domingo.getDayOfWeek() != DayOfWeek.SUNDAY) {
            domingo = domingo.plusDays(1);
        }

        AvailabilityResponse response = consultar(Porte.M, List.of(), domingo);

        assertThat(response.indisponibilidade()).isEqualTo(Indisponibilidade.DIA_INATIVO);
        assertThat(response.slots()).isEmpty();
        // El precio viaja igual: el combo es válido, el día no.
        assertThat(response.precoTotal()).isEqualByComparingTo("59.00");
    }

    @Test
    @DisplayName("una fecha bloqueada devuelve BLOQUEADO")
    void blockedDateIsUnavailable() {
        LocalDate quarta = proximaQuarta();
        ScheduleBlock block = new ScheduleBlock();
        block.setTenantId(tenantId);
        block.setDataDesde(quarta);
        block.setDataHasta(quarta);
        block.setMotivo("Feriado de teste");
        scheduleBlockRepository.save(block);

        assertThat(consultar(Porte.M, List.of(), quarta).indisponibilidade())
                .isEqualTo(Indisponibilidade.BLOQUEADO);
    }

    @Test
    @DisplayName("fecha pasada o más allá de anticipacao_max_dias devuelve FORA_DA_JANELA")
    void outsideWindowIsUnavailable() {
        LocalDate hoje = LocalDate.now(SlotGrid.ZONE_ID);

        assertThat(consultar(Porte.M, List.of(), hoje.minusDays(1)).indisponibilidade())
                .isEqualTo(Indisponibilidade.FORA_DA_JANELA);
        assertThat(consultar(Porte.M, List.of(), hoje.plusDays(61)).indisponibilidade())
                .isEqualTo(Indisponibilidade.FORA_DA_JANELA);
    }

    // ---- capacidad y ocupación ------------------------------------------

    @Test
    @DisplayName("con capacidade 2, el tercer turno en el mismo horario cierra el slot")
    void capacityClosesSlotOnlyWhenFull() {
        LocalDate quarta = proximaQuarta();

        assertThat(disponivel(consultar(Porte.M, List.of(), quarta), "10:00")).isTrue();

        turno(quarta, LocalTime.of(10, 0), 60, AppointmentStatus.PENDING);
        assertThat(disponivel(consultar(Porte.M, List.of(), quarta), "10:00")).isTrue();

        turno(quarta, LocalTime.of(10, 0), 60, AppointmentStatus.CONFIRMED);
        assertThat(disponivel(consultar(Porte.M, List.of(), quarta), "10:00")).isFalse();
    }

    @Test
    @DisplayName("un turno CANCELLED no ocupa cupo")
    void cancelledDoesNotOccupy() {
        LocalDate quarta = proximaQuarta();

        turno(quarta, LocalTime.of(10, 0), 60, AppointmentStatus.CANCELLED);
        turno(quarta, LocalTime.of(10, 0), 60, AppointmentStatus.CANCELLED);
        turno(quarta, LocalTime.of(10, 0), 60, AppointmentStatus.CANCELLED);

        assertThat(disponivel(consultar(Porte.M, List.of(), quarta), "10:00")).isTrue();
    }

    @Test
    @DisplayName("un turno de 90 min cierra los slots posteriores parcialmente solapados")
    void longAppointmentBlocksPartiallyOverlappingSlots() {
        LocalDate quarta = proximaQuarta();

        // Dos turnos de 90 min desde las 09:00 agotan la capacidad hasta las 10:30.
        turno(quarta, LocalTime.of(9, 0), 90, AppointmentStatus.CONFIRMED);
        turno(quarta, LocalTime.of(9, 0), 90, AppointmentStatus.CONFIRMED);

        AvailabilityResponse response = consultar(Porte.M, List.of(), quarta);

        assertThat(disponivel(response, "09:00")).isFalse();
        assertThat(disponivel(response, "09:30")).isFalse();   // arranca dentro del turno
        assertThat(disponivel(response, "10:00")).isFalse();   // termina 11:00, pisa hasta 10:30
        assertThat(disponivel(response, "10:30")).isTrue();    // arranca justo al terminar
    }

    @Test
    @DisplayName("un turno que termina justo cuando empieza el slot no lo ocupa (semiabierto)")
    void adjacentAppointmentDoesNotOccupy() {
        LocalDate quarta = proximaQuarta();

        turno(quarta, LocalTime.of(9, 0), 60, AppointmentStatus.CONFIRMED);
        turno(quarta, LocalTime.of(9, 0), 60, AppointmentStatus.CONFIRMED);

        AvailabilityResponse response = consultar(Porte.M, List.of(), quarta);

        assertThat(disponivel(response, "09:00")).isFalse();
        assertThat(disponivel(response, "10:00")).isTrue();
    }

    @Test
    @DisplayName("los turnos de otro tenant no afectan la disponibilidad")
    void otherTenantsDoNotAffectAvailability() {
        LocalDate quarta = proximaQuarta();

        UUID outroTenant = novoTenant(CAPACIDADE);
        for (int i = 0; i < 5; i++) {
            Appointment alheio = novoTurno(quarta, LocalTime.of(10, 0), 60, AppointmentStatus.CONFIRMED);
            alheio.setTenantId(outroTenant);
            appointmentRepository.save(alheio);
        }

        assertThat(disponivel(consultar(Porte.M, List.of(), quarta), "10:00")).isTrue();
    }

    // ---- horario de atención --------------------------------------------

    @Test
    @DisplayName("el último slot respeta el cierre: sábado cierra 19:00")
    void lastSlotRespectsSaturdayClosing() {
        LocalDate sabado = proximaQuarta();
        while (sabado.getDayOfWeek() != DayOfWeek.SATURDAY) {
            sabado = sabado.plusDays(1);
        }

        List<String> slots = horarios(consultar(Porte.G, List.of(), sabado)); // 90 min

        assertThat(slots).contains("17:30");           // 17:30 + 90 = 19:00 exacto
        assertThat(slots).doesNotContain("18:00");     // 18:00 + 90 = 19:30, excede
    }

    @Test
    @DisplayName("la grilla arranca en la abertura y avanza de 30 en 30")
    void gridStartsAtOpeningEvery30Minutes() {
        List<String> slots = horarios(consultar(Porte.M, List.of(), proximaQuarta()));

        assertThat(slots).startsWith("09:00", "09:30", "10:00");
    }

    // ---- helpers ---------------------------------------------------------

    private AvailabilityResponse consultar(Porte porte, List<Long> addonIds, LocalDate data) {
        return availabilityService.getAvailability(
                tenantId, new AvailabilityQuery(banhoBase.getId(), porte, addonIds, data));
    }

    private static List<String> horarios(AvailabilityResponse response) {
        return response.slots().stream().map(SlotDto::horario).toList();
    }

    private static boolean disponivel(AvailabilityResponse response, String horario) {
        return response.slots().stream()
                .filter(s -> s.horario().equals(horario))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Slot não ofertado: " + horario))
                .disponivel();
    }

    /** Un miércoles con margen sobrado contra la anticipación mínima y la máxima. */
    private static LocalDate proximaQuarta() {
        LocalDate data = LocalDate.now(SlotGrid.ZONE_ID).plusDays(7);
        while (data.getDayOfWeek() != DayOfWeek.WEDNESDAY) {
            data = data.plusDays(1);
        }
        return data;
    }

    private UUID novoTenant(int capacidade) {
        Tenant tenant = new Tenant();
        tenant.setId(UuidV7.generate());
        tenant.setNome("Tenant de Teste");
        tenant.setWhatsappDestino("+5555999990000");
        tenant.setConfig(Map.of(
                "capacidade_atendimento", capacidade,
                "anticipacao_min_horas", 1,
                "anticipacao_max_dias", 60));
        return tenantRepository.save(tenant).getId();
    }

    private void businessHours(UUID tenant, short dia, boolean activo, LocalTime abertura, LocalTime fechamento) {
        BusinessHours hours = new BusinessHours();
        hours.setTenantId(tenant);
        hours.setDiaSemana(dia);
        hours.setActivo(activo);
        hours.setAbertura(abertura);
        hours.setFechamento(fechamento);
        businessHoursRepository.save(hours);
    }

    private ServiceOffering servico(UUID tenant, ServiceType type, String nome) {
        ServiceOffering service = new ServiceOffering();
        service.setTenantId(tenant);
        service.setType(type);
        service.setNome(nome);
        return serviceOfferingRepository.save(service);
    }

    private void pricing(ServiceOffering service, Porte size, String price, int durationMinutes) {
        ServicePricing pricing = new ServicePricing();
        pricing.setService(service);
        pricing.setSize(size);
        pricing.setPrice(new BigDecimal(price));
        pricing.setDurationMinutes(durationMinutes);
        servicePricingRepository.save(pricing);
    }

    private void turno(LocalDate data, LocalTime hora, int duracaoMinutes, AppointmentStatus status) {
        appointmentRepository.save(novoTurno(data, hora, duracaoMinutes, status));
    }

    private Appointment novoTurno(LocalDate data, LocalTime hora, int duracaoMinutes, AppointmentStatus status) {
        Instant inicio = ZonedDateTime.of(data, hora, SlotGrid.ZONE_ID).toInstant();

        Appointment appointment = new Appointment();
        appointment.setPublicId(UuidV7.generate());
        appointment.setTenantId(tenantId);
        appointment.setBaseService(banhoBase);
        appointment.setSize(Porte.M);
        appointment.setStartAt(inicio);
        appointment.setEndAt(inicio.plus(duracaoMinutes, ChronoUnit.MINUTES));
        appointment.setStatus(status);
        appointment.setClienteNome("Cliente de Teste");
        appointment.setClienteTelefone("+55 55 99123-4567");
        appointment.setClienteTelefoneNorm("5555991234567");
        appointment.setPetNome("Thor");
        appointment.setBasePriceSnapshot(new BigDecimal("59.00"));
        appointment.setTotalPriceSnapshot(new BigDecimal("59.00"));
        appointment.setTotalDurationMinutes(duracaoMinutes);
        return appointment;
    }
}
