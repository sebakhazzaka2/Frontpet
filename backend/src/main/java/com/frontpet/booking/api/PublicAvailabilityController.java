package com.frontpet.booking.api;

import com.frontpet.booking.AvailabilityService;
import com.frontpet.booking.domain.Porte;
import com.frontpet.booking.dto.AvailabilityQuery;
import com.frontpet.booking.dto.AvailabilityResponse;
import com.frontpet.tenant.CurrentTenant;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * {@code GET /api/v1/availability} (tarea 5.4). Delega toda la lógica en
 * {@link AvailabilityService} — el controller solo arma el
 * {@link AvailabilityQuery} desde los query params.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PublicAvailabilityController {

    private final AvailabilityService availabilityService;
    private final CurrentTenant currentTenant;

    @GetMapping("/availability")
    public AvailabilityResponse getAvailability(
            @RequestParam Long baseServiceId,
            @RequestParam Porte porte,
            @RequestParam(required = false) List<Long> addonIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        AvailabilityQuery query = new AvailabilityQuery(baseServiceId, porte, addonIds, data);
        return availabilityService.getAvailability(currentTenant.id(), query);
    }
}
