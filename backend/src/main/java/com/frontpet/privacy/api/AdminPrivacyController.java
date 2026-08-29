package com.frontpet.privacy.api;

import com.frontpet.identity.domain.AdminUser;
import com.frontpet.privacy.PrivacyService;
import com.frontpet.privacy.dto.ErasurePreview;
import com.frontpet.privacy.dto.ErasureRequest;
import com.frontpet.privacy.dto.ErasureResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Direito de eliminação LGPD (tarea 7.14, ADR 023) — protegido só pelo
 * {@code anyRequest().authenticated()} de {@code SecurityConfig}, mesmo
 * critério que todo {@code Admin*Controller} do repo (sem regra própria).
 */
@RestController
@RequestMapping("/api/v1/admin/privacy")
@RequiredArgsConstructor
public class AdminPrivacyController {

    private final PrivacyService privacyService;

    /**
     * Preview ANTES de anonimizar — a operação é irreversível (apaga o
     * próprio telefone que identifica o titular), por isso não é um
     * {@code POST} direto sem passo de confirmação.
     */
    @GetMapping("/preview")
    public ErasurePreview preview(@AuthenticationPrincipal AdminUser admin,
                                   @RequestParam String telefone) {
        return privacyService.preview(admin.getTenantId(), telefone);
    }

    @PostMapping("/anonymize")
    public ErasureResult anonymize(@AuthenticationPrincipal AdminUser admin,
                                    @Valid @RequestBody ErasureRequest request) {
        return privacyService.anonymize(admin.getTenantId(), admin.getId(), request.clienteTelefone());
    }
}
