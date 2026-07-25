package com.frontpet.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resuelve de qué tenant es la request en curso.
 *
 * <p>Hoy devuelve siempre el mismo, leído de configuración: MVP1 es
 * single-tenant. Pero los endpoints públicos <b>no tienen JWT</b> del cual
 * sacarlo, así que hace falta alguna fuente igual.
 *
 * <p>El punto de encapsularlo acá es que cuando el sistema pase a multi-tenant
 * —tenant del JWT para el admin, del subdominio para lo público— cambia el
 * cuerpo de este método y nada más. Los services ya reciben el {@code tenantId}
 * como parámetro, no lo van a buscar solos.
 */
@Component
public class CurrentTenant {

    private final UUID tenantId;

    public CurrentTenant(@Value("${frontpet.tenant.id}") UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID id() {
        return tenantId;
    }
}
