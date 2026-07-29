package com.frontpet.booking;

import com.frontpet.booking.domain.Porte;
import com.frontpet.booking.dto.AvailabilityQuery;
import com.frontpet.booking.dto.AvailabilityResponse;
import com.frontpet.booking.dto.ComboPricing;

import java.util.List;
import java.util.UUID;

public interface AvailabilityService {

    AvailabilityResponse getAvailability(UUID tenantId, AvailabilityQuery query);

    /**
     * Resuelve precio y duración del combo (base + adicionais) para un porte,
     * validando tipos y pertenencia al tenant.
     *
     * <p>Expuesto en la interfaz porque {@code AppointmentServiceImpl}
     * (Bloque E) necesita el mismo cálculo para congelar los snapshots al
     * crear el turno. Duplicarlo ahí sería la vía más corta a que
     * disponibilidad y reserva discrepen.
     *
     * @throws IllegalArgumentException si el base no es {@code BASE}, si algún
     *                                  adicional no es {@code ADDON}, o si
     *                                  alguno no existe / está inactivo / no
     *                                  tiene tarifa para ese porte
     */
    ComboPricing resolveCombo(UUID tenantId, Long baseServiceId, List<Long> addonIds, Porte porte);
}
