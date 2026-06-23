package com.digitrans.supply_service.supply.controller;

import com.digitrans.supply_service.supply.dto.ApiResponse;
import com.digitrans.supply_service.supply.dto.StockReport;
import com.digitrans.supply_service.supply.entity.WarehouseStock;
import com.digitrans.supply_service.supply.service.WarehouseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/supply/warehouse/stock")
@RequiredArgsConstructor
@Tag(name = "Supply Chain - Traçabilité Cacao/Café")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public ResponseEntity<ApiResponse<StockReport>> getStockReport() {
        StockReport report = warehouseService.getStockReport();
        return ResponseEntity.ok(new ApiResponse<>(true, "Stock report generated", report));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseStock>> receiveStock(@RequestBody Map<String, Object> request) {
        String warehouseCode = request.get("warehouseCode").toString();
        String warehouseName = request.get("warehouseName").toString();
        String location = request.get("location").toString();
        Long batchId = Long.valueOf(request.get("batchId").toString());
        Integer quantityKg = Integer.valueOf(request.get("quantityKg").toString());
        LocalDate expiryDate = LocalDate.parse(request.get("expiryDate").toString());

        WarehouseStock stock = warehouseService.receiveStock(warehouseCode, warehouseName, location,
                batchId, quantityKg, expiryDate);
        return ResponseEntity.ok(new ApiResponse<>(true, "Stock received", stock));
    }

    @PutMapping("/{stockId}/reserve")
    public ResponseEntity<ApiResponse<WarehouseStock>> reserveStock(@PathVariable Long stockId) {
        WarehouseStock stock = warehouseService.reserveStock(stockId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Stock reserved", stock));
    }

    @PutMapping("/{stockId}/dispatch")
    public ResponseEntity<ApiResponse<WarehouseStock>> dispatchStock(@PathVariable Long stockId) {
        WarehouseStock stock = warehouseService.dispatchStock(stockId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Stock dispatched", stock));
    }
}
