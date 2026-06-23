package com.digitrans.supply_service.supply.dto;

import com.digitrans.supply_service.supply.entity.BatchStatus;
import com.digitrans.supply_service.supply.entity.QualityGrade;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TraceabilityResponse(
        Long batchId,
        String batchCode,
        Long plantationId,
        String plantationName,
        LocalDate harvestDate,
        Integer quantityKg,
        QualityGrade qualityGrade,
        BatchStatus status,
        String currentLocation,
        String blockchainTxHash,
        LocalDateTime createdAt,
        List<EventResponse> events,
        String verificationStatus
) {}
