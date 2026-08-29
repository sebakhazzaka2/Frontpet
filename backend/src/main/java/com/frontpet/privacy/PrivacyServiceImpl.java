package com.frontpet.privacy;

import com.frontpet.booking.AppointmentService;
import com.frontpet.common.PhoneNormalizer;
import com.frontpet.orders.OrderService;
import com.frontpet.privacy.domain.PrivacyErasureLog;
import com.frontpet.privacy.domain.PrivacyErasureLogRepository;
import com.frontpet.privacy.dto.ErasurePreview;
import com.frontpet.privacy.dto.ErasureResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrivacyServiceImpl implements PrivacyService {

    private final OrderService orderService;
    private final AppointmentService appointmentService;
    private final PrivacyErasureLogRepository privacyErasureLogRepository;

    @Override
    @Transactional(readOnly = true)
    public ErasurePreview preview(UUID tenantId, String clienteTelefone) {
        String norm = PhoneNormalizer.normalizeBr(clienteTelefone);
        List<UUID> orderIds = orderService.findPublicIdsByPhone(tenantId, norm);
        List<UUID> appointmentIds = appointmentService.findPublicIdsByPhone(tenantId, norm);
        if (orderIds.isEmpty() && appointmentIds.isEmpty()) {
            throw new PrivacyRecordNotFoundException("Nenhum registro encontrado para este telefone.");
        }
        return new ErasurePreview(orderIds, appointmentIds);
    }

    @Override
    @Transactional
    public ErasureResult anonymize(UUID tenantId, Long adminUserId, String clienteTelefone) {
        String norm = PhoneNormalizer.normalizeBr(clienteTelefone);

        int ordersAnonymized = orderService.anonymizeByPhone(tenantId, norm);
        int appointmentsAnonymized = appointmentService.anonymizeByPhone(tenantId, norm);

        if (ordersAnonymized == 0 && appointmentsAnonymized == 0) {
            // Idempotência (docstring de PrivacyService#anonymize): uma
            // segunda chamada com o mesmo telefone já não encontra nada,
            // porque cliente_telefone_norm virou NULL na primeira.
            throw new PrivacyRecordNotFoundException("Nenhum registro encontrado para este telefone.");
        }

        privacyErasureLogRepository.save(new PrivacyErasureLog(
                tenantId, adminUserId, sha256(norm), ordersAnonymized, appointmentsAnonymized));

        return new ErasureResult(ordersAnonymized, appointmentsAnonymized);
    }

    /**
     * Hash do telefone JÁ normalizado — guardar isto em vez do telefone é o
     * ponto inteiro do log de auditoria (ADR 023): registrar que a
     * eliminação aconteceu sem reter o identificador eliminado.
     */
    private String sha256(String normalizedPhone) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizedPhone.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 é parte obrigatória de toda JVM (JLS) — nunca acontece.
            throw new IllegalStateException("SHA-256 indisponível na JVM.", e);
        }
    }
}
