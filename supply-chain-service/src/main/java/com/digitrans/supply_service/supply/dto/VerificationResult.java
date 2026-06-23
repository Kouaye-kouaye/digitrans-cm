package com.digitrans.supply_service.supply.dto;

public record VerificationResult(
        boolean isValid,
        String status,
        Integer totalEvents,
        Integer validEvents,
        String message
) {}
