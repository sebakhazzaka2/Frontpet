package com.frontpet.catalog;

import com.frontpet.catalog.dto.PresignedUploadRequest;
import com.frontpet.catalog.dto.PresignedUploadResponse;

import java.util.UUID;

public interface R2StorageService {

    /**
     * Firma una URL de subida para la foto de un producto.
     *
     * @throws IllegalArgumentException mime no permitido o tamaño excede el tope
     */
    PresignedUploadResponse presignProductImageUpload(UUID tenantId, PresignedUploadRequest request);
}
