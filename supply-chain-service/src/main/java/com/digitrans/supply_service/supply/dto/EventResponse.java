package com.digitrans.supply_service.supply.dto;

import com.digitrans.supply_service.supply.entity.EventType;

import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        EventType eventType,
        String location,
        String operatorName,
        LocalDateTime eventAt,
        String metadata,
        String hashSignature
) {}
