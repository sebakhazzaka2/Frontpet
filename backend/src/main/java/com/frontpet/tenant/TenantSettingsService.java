package com.frontpet.tenant;

import com.frontpet.tenant.dto.BookingSettings;

import java.util.UUID;

public interface TenantSettingsService {

    BookingSettings booking(UUID tenantId);
}
