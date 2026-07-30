package com.frontpet.booking;

import com.frontpet.booking.domain.AppointmentRepository;
import com.frontpet.booking.domain.BusinessHours;
import com.frontpet.booking.domain.BusinessHoursRepository;
import com.frontpet.booking.domain.Porte;
import com.frontpet.booking.domain.ServiceOffering;
import com.frontpet.booking.domain.ServiceOfferingRepository;
import com.frontpet.booking.domain.ServicePricing;
import com.frontpet.booking.dto.CreateAppointmentRequest;
import com.frontpet.AbstractIntegrationTest;
import com.frontpet.common.UuidV7;
import com.frontpet.tenant.domain.Tenant;
import com.frontpet.tenant.domain.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Control de concurrencia del último cupo (tarea 5.6, AC 11). El test más
 * frágil del sprint: verifica que {@code pg_advisory_xact_lock} evita el
 * overbooking cuando N clientes piden el mismo horario a la vez.
 *
 * <p><b>Sin {@code @Transactional}</b> a propósito, y es la razón de ser de
 * toda la ceremonia de esta clase: hacen falta <b>commits reales</b>. Con el
 * test dentro de una transacción, los 6 threads compartirían o no verían nada
 * entre sí y el test no probaría nada. Como consecuencia, la limpieza es
 * manual ({@link #cleanUp()}) — sin ella los turnos quedarían en el
 * contenedor singleton y contaminarían las otras clases de test.
 *
 * <p><b>Por qué el lock alcanza</b>: cada thread toma el advisory lock, recuenta
 * los solapados y solo entonces inserta. El lock se libera en el commit (es
 * {@code _xact_}), así que el thread siguiente arranca su {@code SELECT} cuando
 * el INSERT anterior ya está commiteado — y bajo {@code READ COMMITTED} (default
 * de Postgres) cada sentencia toma un snapshot fresco, así que lo ve. Esa es
 * exactamente la propiedad que hace correcta la combinación sin lógica de
 * reintento; con {@code SERIALIZABLE} haría falta reintentar sobre {@code 40001}
 * (ADR 020 §4).
 *
 * <p>⚠️ <b>Cómo verificar que este test muerde</b>: comentar la llamada a
 * {@code lockTenantDay(...)} en {@code AppointmentServiceImpl.create} debe
 * hacerlo fallar. Verificado al escribirlo, 3 corridas de 3: sin el lock se
 * crean <b>6 turnos en vez de 2</b> — los 6 threads leen "0 ocupados" antes de
 * que ninguno haya insertado. El fallo resultó <b>consistente</b>, no
 * intermitente como anticipaba el plan de sprint, porque el
 * {@link CyclicBarrier} alinea las 6 lecturas dentro de la misma ventana.
 */
@SpringBootTest
class AppointmentConcurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final int CAPACIDADE = 2;
    private static final int THREADS = 6;
    private static final LocalTime HORARIO_DISPUTADO = LocalTime.of(10, 0);

    @Autowired AppointmentService appointmentService;
    @Autowired TenantRepository tenantRepository;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;
    @Autowired BusinessHoursRepository businessHoursRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private Long banhoBaseId;
    private LocalDate data;

    /** Resultado de un intento de reserva — separa el rechazo legítimo de un bug. */
    private enum Outcome { CREATED, NO_CAPACITY, UNEXPECTED_ERROR }

    @BeforeEach
    void seed() {
        // Cada save() commitea por su cuenta (la clase no es @Transactional),
        // así que los threads ven este seed sin necesidad de flush explícito.
        Tenant tenant = new Tenant();
        tenant.setId(UuidV7.generate());
        tenant.setNome("Tenant Concorrência");
        tenant.setWhatsappDestino("+5555999990000");
        tenant.setConfig(java.util.Map.of(
                "capacidade_atendimento", CAPACIDADE,
                "anticipacao_min_horas", 1,
                "anticipacao_max_dias", 60));
        tenantRepository.save(tenant);
        tenantId = tenant.getId();

        for (int dia = 1; dia <= 7; dia++) {
            BusinessHours hours = new BusinessHours();
            hours.setTenantId(tenantId);
            hours.setDiaSemana((short) dia);
            hours.setActivo(true);
            hours.setAbertura(LocalTime.of(9, 0));
            hours.setFechamento(LocalTime.of(17, 0));
            businessHoursRepository.save(hours);
        }

        ServiceOffering banhoBase = new ServiceOffering();
        banhoBase.setTenantId(tenantId);
        banhoBase.setType(com.frontpet.booking.domain.ServiceType.BASE);
        banhoBase.setNome("Banho Concorrência");
        ServicePricing pricing = new ServicePricing();
        pricing.setService(banhoBase);
        pricing.setSize(Porte.M);
        pricing.setPrice(new BigDecimal("59.00"));
        pricing.setDurationMinutes(60);
        banhoBase.getPricing().add(pricing);
        serviceOfferingRepository.save(banhoBase);
        banhoBaseId = banhoBase.getId();

        data = proximaQuarta();
    }

    /**
     * Limpieza manual, en orden de FK: {@code appointments} cascadea a
     * {@code appointment_addons}, {@code services} a {@code service_pricing};
     * {@code business_hours} y {@code services} son RESTRICT contra
     * {@code tenants}, así que el tenant va último.
     */
    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM appointments WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM services WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM business_hours WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM tenants WHERE id = ?", tenantId);
    }

    @Test
    @DisplayName("6 threads simultáneos contra 2 cupos crean exactamente 2 turnos y rechazan 4")
    void sixThreadsAgainstTwoSlotsCreateExactlyTwo() throws Exception {
        CyclicBarrier largada = new CyclicBarrier(THREADS);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        try {
            List<Future<Outcome>> futures = new ArrayList<>(THREADS);
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(reservar(largada, "Cliente " + i)));
            }

            long criados = 0;
            long semCupo = 0;
            List<Outcome> outcomes = new ArrayList<>(THREADS);
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            for (Outcome outcome : outcomes) {
                switch (outcome) {
                    case CREATED -> criados++;
                    case NO_CAPACITY -> semCupo++;
                    case UNEXPECTED_ERROR -> { /* contado por el assert de abajo */ }
                }
            }

            // Un error inesperado (NPE, deadlock, timeout de pool) NO cuenta como
            // rechazo por capacidad: sin esta distinción el test pasaría en falso.
            assertThat(outcomes).doesNotContain(Outcome.UNEXPECTED_ERROR);
            assertThat(criados).as("turnos creados").isEqualTo(CAPACIDADE);
            assertThat(semCupo).as("rechazos por falta de cupo").isEqualTo(THREADS - CAPACIDADE);

            // La verdad está en la DB, no en lo que devolvieron los threads.
            Integer persistidos = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM appointments WHERE tenant_id = ? AND status <> 'CANCELLED'",
                    Integer.class, tenantId);
            assertThat(persistidos).as("turnos realmente persistidos").isEqualTo(CAPACIDADE);
        } finally {
            pool.shutdownNow();
        }
    }

    private Callable<Outcome> reservar(CyclicBarrier largada, String clienteNome) {
        return () -> {
            try {
                // Todos los threads esperan acá y salen juntos: sin el barrier
                // el primero podría terminar antes de que arranque el último y
                // no habría carrera que probar.
                largada.await(15, TimeUnit.SECONDS);
                appointmentService.create(tenantId, request(clienteNome));
                return Outcome.CREATED;
            } catch (SlotUnavailableException e) {
                return Outcome.NO_CAPACITY;
            } catch (Exception e) {
                // No se traga el error: se reporta como categoría propia para
                // que el assert lo delate en vez de disfrazarse de rechazo.
                System.err.println("Erro inesperado em " + clienteNome + ": " + e);
                return Outcome.UNEXPECTED_ERROR;
            }
        };
    }

    private CreateAppointmentRequest request(String clienteNome) {
        return new CreateAppointmentRequest(
                banhoBaseId,
                List.of(),
                Porte.M,
                data,
                HORARIO_DISPUTADO,
                clienteNome,
                "(55) 99123-4567",
                "Thor",
                null,
                null,
                null);
    }

    private static LocalDate proximaQuarta() {
        LocalDate d = LocalDate.now(SlotGrid.ZONE_ID).plusDays(7);
        while (d.getDayOfWeek() != DayOfWeek.WEDNESDAY) {
            d = d.plusDays(1);
        }
        return d;
    }
}
