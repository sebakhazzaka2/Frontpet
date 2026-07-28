package com.frontpet.orders.api;

import com.frontpet.orders.OrderService;
import com.frontpet.orders.dto.CreateOrderRequest;
import com.frontpet.orders.dto.OrderDetail;
import com.frontpet.tenant.CurrentTenant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Checkout público, anónimo (tarea 4.8). Único endpoint de escritura del
 * proyecto que no requiere login — protegido en cambio por
 * {@code OrderRateLimitFilter} y por el campo honeypot (tarea 4.15).
 *
 * <p>El tenant sale de {@link CurrentTenant}, igual que
 * {@code PublicCatalogController}: no hay JWT del cual sacarlo.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PublicOrderController {

    private final OrderService orderService;
    private final CurrentTenant currentTenant;

    @PostMapping("/orders")
    public ResponseEntity<OrderDetail> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderDetail created = orderService.create(currentTenant.id(), request);
        return ResponseEntity.status(201).body(created);
    }
}
