package com.frontpet.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Campos de auditoría compartidos por todas las entidades del dominio.
 *
 * <p>{@code @MappedSuperclass} = esta clase NO es una tabla. Le dice a JPA que
 * sus columnas se agreguen a la tabla de cada entidad que herede de acá.
 * {@code Product} sigue mapeando a {@code products}, pero se lleva puestas las
 * dos columnas de fecha sin declararlas.
 *
 * <p>Postgres no tiene el {@code ON UPDATE CURRENT_TIMESTAMP} de MySQL, así que
 * el {@code updated_at} lo bumpea Hibernate a nivel entidad. En la DB igual
 * quedan los {@code DEFAULT now()} para cubrir los inserts de migraciones y
 * seeds, que no pasan por Hibernate. Ver ADR 013 §7.
 *
 * <p>Solo getters a propósito: las fechas las maneja Hibernate, nadie las setea
 * a mano.
 */
@MappedSuperclass
@Getter
public abstract class Auditable {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
