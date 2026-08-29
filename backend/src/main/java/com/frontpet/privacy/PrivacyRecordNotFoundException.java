package com.frontpet.privacy;

/**
 * Nenhum {@code order}/{@code appointment} do tenant tem esse telefone —
 * ou porque nunca existiu, ou porque um pedido anterior de eliminação já
 * anonimizou os registros (que passam a ter {@code cliente_telefone_norm
 * = NULL} e deixam de dar match).
 */
public class PrivacyRecordNotFoundException extends RuntimeException {

    public PrivacyRecordNotFoundException(String message) {
        super(message);
    }
}
