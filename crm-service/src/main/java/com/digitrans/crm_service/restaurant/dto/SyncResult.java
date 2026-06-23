package com.digitrans.crm_service.restaurant.dto;

import java.util.List;

public record SyncResult(
        int created,
        int skipped,
        List<String> errors
) {}
