package com.digitrans.supply_service.supply.dto;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        String timestamp
) {
    public ApiResponse(boolean success, String message, T data) {
        this(success, message, data, Instant.now().toString());
    }
}
