package com.frontpet.booking.api;

import com.frontpet.booking.AppointmentService;
import com.frontpet.booking.domain.AppointmentStatus;
import com.frontpet.booking.dto.AdminAppointmentDetail;
import com.frontpet.booking.dto.CreateManualAppointmentRequest;
import com.frontpet.booking.dto.ManualAppointmentResult;
import com.frontpet.booking.dto.TempoExtraResult;
import com.frontpet.booking.dto.UpdateAppointmentStatusRequest;
import com.frontpet.booking.dto.UpdateTempoExtraRequest;
import com.frontpet.identity.domain.AdminUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Gestão de turnos do admin (tareas 5.5/5.6). Protegido por
 * {@code anyRequest().authenticated()} de {@code SecurityConfig} — mismo
 * criterio que {@code AdminOrderController}; el tenant sale de
 * {@link AdminUser}, nunca de {@code CurrentTenant}.
 */
@RestController
@RequestMapping("/api/v1/admin/appointments")
@RequiredArgsConstructor
public class AdminAppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public List<AdminAppointmentDetail> list(
            @AuthenticationPrincipal AdminUser admin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) AppointmentStatus status) {
        return appointmentService.listForAdmin(admin.getTenantId(), data, desde, hasta, status);
    }

    @PostMapping
    public ResponseEntity<ManualAppointmentResult> createManual(
            @AuthenticationPrincipal AdminUser admin,
            @Valid @RequestBody CreateManualAppointmentRequest request) {
        ManualAppointmentResult created = appointmentService.createManual(admin.getTenantId(), request);
        return ResponseEntity.status(201).body(created);
    }

    @PatchMapping("/{publicId}/status")
    public AdminAppointmentDetail updateStatus(
            @AuthenticationPrincipal AdminUser admin,
            @PathVariable UUID publicId,
            @Valid @RequestBody UpdateAppointmentStatusRequest request) {
        return appointmentService.updateStatus(admin.getTenantId(), publicId, request.status());
    }

    @PatchMapping("/{publicId}/tempo-extra")
    public TempoExtraResult updateTempoExtra(
            @AuthenticationPrincipal AdminUser admin,
            @PathVariable UUID publicId,
            @Valid @RequestBody UpdateTempoExtraRequest request) {
        return appointmentService.updateTempoExtra(admin.getTenantId(), publicId, request.tempoExtra());
    }
}
