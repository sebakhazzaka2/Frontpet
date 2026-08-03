package com.frontpet.booking.api;

import com.frontpet.booking.ScheduleService;
import com.frontpet.booking.dto.BusinessHoursDetail;
import com.frontpet.booking.dto.CreateScheduleBlockRequest;
import com.frontpet.booking.dto.ScheduleBlockDetail;
import com.frontpet.booking.dto.UpsertBusinessHoursRequest;
import com.frontpet.identity.domain.AdminUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin de horarios y bloqueos (tarea 5.7, Bloque G). Mismo patrón que
 * {@link AdminServiceController}: tenant desde {@link AdminUser}, nunca de
 * {@code CurrentTenant}.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/business-hours")
    public List<BusinessHoursDetail> listBusinessHours(@AuthenticationPrincipal AdminUser admin) {
        return scheduleService.listBusinessHours(admin.getTenantId());
    }

    @GetMapping("/schedule-blocks")
    public List<ScheduleBlockDetail> listScheduleBlocks(@AuthenticationPrincipal AdminUser admin) {
        return scheduleService.listScheduleBlocks(admin.getTenantId());
    }

    @PutMapping("/business-hours")
    public List<BusinessHoursDetail> upsertBusinessHours(@AuthenticationPrincipal AdminUser admin,
                                                          @Valid @RequestBody List<UpsertBusinessHoursRequest> request) {
        return scheduleService.upsertBusinessHours(admin.getTenantId(), request);
    }

    @PostMapping("/schedule-blocks")
    public ResponseEntity<ScheduleBlockDetail> createScheduleBlock(@AuthenticationPrincipal AdminUser admin,
                                                                    @Valid @RequestBody CreateScheduleBlockRequest request) {
        ScheduleBlockDetail created = scheduleService.createScheduleBlock(admin.getTenantId(), request);
        return ResponseEntity.status(201).body(created);
    }

    @DeleteMapping("/schedule-blocks/{id}")
    public ResponseEntity<Void> deleteScheduleBlock(@AuthenticationPrincipal AdminUser admin, @PathVariable Long id) {
        scheduleService.deleteScheduleBlock(admin.getTenantId(), id);
        return ResponseEntity.noContent().build();
    }
}
