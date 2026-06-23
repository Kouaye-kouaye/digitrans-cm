package com.digitrans.supply_service.supply.controller;

import com.digitrans.supply_service.supply.dto.ApiResponse;
import com.digitrans.supply_service.supply.dto.TraceabilityResponse;
import com.digitrans.supply_service.supply.dto.VerificationResult;
import com.digitrans.supply_service.supply.entity.HarvestBatch;
import com.digitrans.supply_service.supply.service.HarvestService;
import com.digitrans.supply_service.supply.service.TraceabilityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/supply/batches")
@RequiredArgsConstructor
@Tag(name = "Supply Chain - Traçabilité Cacao/Café")
public class BatchController {

    private final HarvestService harvestService;
    private final TraceabilityService traceabilityService;

    @PostMapping
    public ResponseEntity<ApiResponse<HarvestBatch>> createBatch(@RequestBody Map<String, Object> request) {
        Long plantationId = Long.valueOf(request.get("plantationId").toString());
        LocalDate harvestDate = LocalDate.parse(request.get("harvestDate").toString());
        Integer quantityKg = Integer.valueOf(request.get("quantityKg").toString());
        String qualityGrade = request.get("qualityGrade").toString();
        String currentLocation = request.get("currentLocation").toString();

        HarvestBatch batch = harvestService.createBatch(plantationId, harvestDate, quantityKg, qualityGrade, currentLocation);
        return ResponseEntity.ok(new ApiResponse<>(true, "Batch created", batch));
    }

    @GetMapping("/{batchCode}/trace")
    public ResponseEntity<ApiResponse<TraceabilityResponse>> getTrace(@PathVariable String batchCode) {
        TraceabilityResponse response = harvestService.getFullTraceability(batchCode);
        return ResponseEntity.ok(new ApiResponse<>(true, "Traceability chain retrieved", response));
    }

    @GetMapping("/{batchCode}/verify")
    public ResponseEntity<ApiResponse<VerificationResult>> verify(@PathVariable String batchCode) {
        HarvestBatch batch = harvestService.getByBatchCode(batchCode);
        VerificationResult result = traceabilityService.verifyIntegrity(batch);
        return ResponseEntity.ok(new ApiResponse<>(true, "Verification completed", result));
    }

    @PutMapping("/{batchCode}/status")
    public ResponseEntity<ApiResponse<HarvestBatch>> updateStatus(
            @PathVariable String batchCode,
            @RequestBody Map<String, Object> request) {
        HarvestBatch batch = harvestService.getByBatchCode(batchCode);
        String newStatus = request.get("status").toString();
        String location = request.get("location").toString();
        String operatorName = request.get("operatorName").toString();

        HarvestBatch updated = harvestService.updateStatus(batch.getId(), newStatus, location, operatorName);
        return ResponseEntity.ok(new ApiResponse<>(true, "Batch status updated", updated));
    }

    @GetMapping("/plantation/{plantationId}")
    public ResponseEntity<ApiResponse<List<HarvestBatch>>> getByPlantationAndDateRange(
            @PathVariable Long plantationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<HarvestBatch> batches;
        if (from != null && to != null) {
            batches = harvestService.getByPlantationAndDateRange(plantationId, from, to);
        } else {
            batches = harvestService.getByPlantationAndDateRange(plantationId, LocalDate.of(2000, 1, 1), LocalDate.now());
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Batches retrieved", batches));
    }
}
