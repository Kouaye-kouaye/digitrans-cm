package com.digitrans.supply_service.supply.service;

import com.digitrans.supply_service.supply.dto.TraceabilityResponse;
import com.digitrans.supply_service.supply.dto.VerificationResult;
import com.digitrans.supply_service.supply.entity.BatchStatus;
import com.digitrans.supply_service.supply.entity.HarvestBatch;
import com.digitrans.supply_service.supply.entity.Plantation;
import com.digitrans.supply_service.supply.repository.HarvestBatchRepository;
import com.digitrans.supply_service.supply.repository.PlantationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HarvestService {

    private final HarvestBatchRepository harvestBatchRepository;
    private final PlantationRepository plantationRepository;
    private final TraceabilityService traceabilityService;

    @Transactional
    public HarvestBatch createBatch(Long plantationId, LocalDate harvestDate, Integer quantityKg, 
                                    String qualityGrade, String currentLocation) {
        Plantation plantation = plantationRepository.findById(plantationId)
                .orElseThrow(() -> new RuntimeException("Plantation not found: " + plantationId));

        String batchCode = generateBatchCode();
        String blockchainTxHash = generateFakeBlockchainHash();

        HarvestBatch batch = HarvestBatch.builder()
                .batchCode(batchCode)
                .plantation(plantation)
                .harvestDate(harvestDate)
                .quantityKg(quantityKg)
                .qualityGrade(Enum.valueOf(com.digitrans.supply_service.supply.entity.QualityGrade.class, qualityGrade))
                .status(BatchStatus.HARVESTED)
                .currentLocation(currentLocation)
                .blockchainTxHash(blockchainTxHash)
                .build();

        HarvestBatch saved = harvestBatchRepository.save(batch);

        // Create initial HARVEST event
        traceabilityService.addEvent(saved, "HARVEST", currentLocation, "System", null);

        return saved;
    }

    @Transactional
    public HarvestBatch updateStatus(Long batchId, String newStatus, String location, String operatorName) {
        HarvestBatch batch = harvestBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found: " + batchId));

        BatchStatus status = BatchStatus.valueOf(newStatus);
        batch.setStatus(status);
        batch.setCurrentLocation(location);

        HarvestBatch updated = harvestBatchRepository.save(batch);

        // Auto-create traceability event based on status
        String eventType = mapStatusToEventType(newStatus);
        traceabilityService.addEvent(updated, eventType, location, operatorName, null);

        return updated;
    }

    public TraceabilityResponse getFullTraceability(String batchCode) {
        HarvestBatch batch = harvestBatchRepository.findByBatchCode(batchCode)
                .orElseThrow(() -> new RuntimeException("Batch not found: " + batchCode));

        VerificationResult verification = traceabilityService.verifyIntegrity(batch);

        return new TraceabilityResponse(
                batch.getId(),
                batch.getBatchCode(),
                batch.getPlantation().getId(),
                batch.getPlantation().getName(),
                batch.getHarvestDate(),
                batch.getQuantityKg(),
                batch.getQualityGrade(),
                batch.getStatus(),
                batch.getCurrentLocation(),
                batch.getBlockchainTxHash(),
                batch.getCreatedAt(),
                traceabilityService.getEventResponses(batch),
                verification.status()
        );
    }

    public HarvestBatch getByBatchCode(String batchCode) {
        return harvestBatchRepository.findByBatchCode(batchCode)
                .orElseThrow(() -> new RuntimeException("Batch not found: " + batchCode));
    }

    public List<HarvestBatch> getByPlantationAndDateRange(Long plantationId, LocalDate from, LocalDate to) {
        return harvestBatchRepository.findByPlantationIdAndHarvestDateBetween(plantationId, from, to);
    }

    private String generateBatchCode() {
        long sequence = harvestBatchRepository.count() + 1;
        LocalDate today = LocalDate.now();
        return String.format("BATCH-%d-%03d", today.getYear(), sequence);
    }

    private String generateFakeBlockchainHash() {
        String hex = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        return "0x" + hex.substring(0, 64);
    }

    private String mapStatusToEventType(String status) {
        return switch (status) {
            case "IN_TRANSIT" -> "TRANSPORT_START";
            case "AT_WAREHOUSE" -> "WAREHOUSE_ARRIVAL";
            case "PROCESSED" -> "PROCESSING";
            case "EXPORTED" -> "EXPORT_READY";
            default -> "HARVEST";
        };
    }
}
