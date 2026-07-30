package com.frontpet.booking.api;

import com.frontpet.booking.AppointmentService;
import com.frontpet.booking.dto.AppointmentDetail;
import com.frontpet.booking.dto.CreateAppointmentRequest;
import com.frontpet.tenant.CurrentTenant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Reserva pública y anónima (tarea 5.5), protegida por
 * {@code AppointmentRateLimitFilter} + honeypot — mismo criterio que
 * {@code PublicOrderController}.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PublicAppointmentController {

    private final AppointmentService appointmentService;
    private final CurrentTenant currentTenant;

    @PostMapping("/appointments")
    public ResponseEntity<AppointmentDetail> create(@Valid @RequestBody CreateAppointmentRequest request) {
        AppointmentDetail created = appointmentService.create(currentTenant.id(), request);
        return ResponseEntity.status(201).body(created);
    }

    /**
     * Vista reducida, sin telefone completo (limitação aberta em
     * docs/booking-api-contracts.md: o UUID v7 do link não é adivinável, mas
     * não é por si só suficiente — deuda documentada, não bloqueante).
     */
    @GetMapping("/appointments/{publicId}")
    public AppointmentDetail get(@PathVariable UUID publicId) {
        return appointmentService.getPublicByPublicId(currentTenant.id(), publicId);
    }
}
