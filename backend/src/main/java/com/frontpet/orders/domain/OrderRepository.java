package com.frontpet.orders.domain;

import com.frontpet.orders.dto.OrderSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Detalle de un pedido, para el checkout response y la vista del admin.
     * {@code @EntityGraph} es seguro acá porque es una sola fila — nunca en
     * el listado paginado (ver {@link #findSummaries}).
     */
    @EntityGraph(attributePaths = "items")
    Optional<Order> findByTenantIdAndPublicId(UUID tenantId, UUID publicId);

    /**
     * Listado paginado del admin. Proyecta directo a {@link OrderSummary} en
     * vez de traer entidades: con {@code open-in-view: false}, cargar
     * {@code Order} con su colección de {@code items} le impediría a Postgres
     * paginar de verdad (mismo motivo documentado en
     * {@code catalog.domain.ProductRepository#findSummaries}).
     *
     * <p>{@code status} es opcional — {@code null} = todos los estados. El
     * filtro va en una sola query (no dos separadas) para no duplicar el
     * bloque {@code new OrderSummary(...)}.
     *
     * <p>{@code itemCount} sale de una subquery correlacionada, no de
     * {@code SIZE(o.items)}: Hibernate no admite {@code SIZE()} dentro de una
     * constructor expression. El {@code CAST(... AS integer)} es necesario
     * porque {@code COUNT()} devuelve {@code Long} y el constructor de
     * {@link OrderSummary} espera {@code int} — sin el cast, Hibernate no
     * encuentra el constructor y falla al arrancar (no en runtime).
     */
    @Query(value = """
            SELECT new com.frontpet.orders.dto.OrderSummary(
                o.publicId,
                o.status,
                o.clienteNome,
                o.clienteTelefone,
                CAST((SELECT COUNT(i) FROM OrderItem i WHERE i.order = o) AS integer),
                o.subtotalSnapshot,
                o.createdAt
            )
            FROM Order o
            WHERE o.tenantId = :tenantId
              AND (:status IS NULL OR o.status = :status)
            """,
            countQuery = """
            SELECT COUNT(o) FROM Order o
            WHERE o.tenantId = :tenantId
              AND (:status IS NULL OR o.status = :status)
            """)
    Page<OrderSummary> findSummaries(
            @Param("tenantId") UUID tenantId,
            @Param("status") OrderStatus status,
            Pageable pageable
    );

    /** Contadores para las tabs del admin (Todos/Pendentes/Confirmados/Cancelados). */
    @Query("""
            SELECT o.status, COUNT(o)
            FROM Order o
            WHERE o.tenantId = :tenantId
            GROUP BY o.status
            """)
    List<Object[]> countGroupedByStatus(@Param("tenantId") UUID tenantId);

    /**
     * Direito de eliminação LGPD (V13, ADR 023). Pedidos já anonimizados têm
     * {@code cliente_telefone_norm = NULL} e nunca dão match aqui — a mesma
     * query serve tanto para o preview quanto, implicitamente, para tornar a
     * anonimização idempotente (rodar de novo não encontra mais nada).
     */
    List<Order> findByTenantIdAndClienteTelefoneNorm(UUID tenantId, String clienteTelefoneNorm);
}
