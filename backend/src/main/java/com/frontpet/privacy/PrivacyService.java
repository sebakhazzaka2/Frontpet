package com.frontpet.privacy;

import com.frontpet.privacy.dto.ErasurePreview;
import com.frontpet.privacy.dto.ErasureResult;

import java.util.UUID;

/**
 * Direito de eliminação LGPD (ADR 023, tarea 7.14) — a única forma de
 * cumprir a promessa que {@code /privacidade} já faz por escrito. Orquestra
 * {@code orders.OrderService} e {@code booking.AppointmentService}, nunca
 * seus repositórios (CLAUDE.md §5: "otros módulos consumen esa interfaz,
 * nunca el repositorio").
 */
public interface PrivacyService {

    /**
     * O que seria anonimizado, sem executar — a anonimização apaga
     * exatamente o dado (telefone) que identifica o titular, então não dá
     * pra "desfazer e conferir" depois.
     *
     * @throws PrivacyRecordNotFoundException se nenhum pedido/turno do
     *                                         tenant tem este telefone
     * @throws IllegalArgumentException        se o telefone é inválido
     */
    ErasurePreview preview(UUID tenantId, String clienteTelefone);

    /**
     * Anonimiza todo pedido/turno do tenant com este telefone e registra a
     * operação em {@code privacy_erasure_log}. Idempotente: uma segunda
     * chamada com o mesmo telefone não encontra mais nada (404).
     *
     * @throws PrivacyRecordNotFoundException se nenhum pedido/turno do
     *                                         tenant tem este telefone
     * @throws IllegalArgumentException        se o telefone é inválido
     */
    ErasureResult anonymize(UUID tenantId, Long adminUserId, String clienteTelefone);
}
