package com.digitrans.supply_service.supply.service;

import com.digitrans.supply_service.supply.dto.StockReport;
import com.digitrans.supply_service.supply.entity.HarvestBatch;
import com.digitrans.supply_service.supply.entity.WarehouseStock;
import com.digitrans.supply_service.supply.entity.WarehouseStockStatus;
import com.digitrans.supply_service.supply.repository.HarvestBatchRepository;
import com.digitrans.supply_service.supply.repository.WarehouseStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseStockRepository warehouseStockRepository;
    private final HarvestBatchRepository harvestBatchRepository;

    @Transactional
    public WarehouseStock receiveStock(String warehouseCode, String warehouseName, String location,
                                       Long batchId, Integer quantityKg, LocalDate expiryDate) {
        HarvestBatch batch = harvestBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found: " + batchId));

        WarehouseStock stock = WarehouseStock.builder()
                .warehouseCode(warehouseCode)
                .warehouseName(warehouseName)
                .location(location)
                .batch(batch)
                .quantityKg(quantityKg)
                .arrivalDate(LocalDate.now())
                .expiryDate(expiryDate)
                .status(WarehouseStockStatus.AVAILABLE)
                .build();

        return warehouseStockRepository.save(stock);
    }

    @Transactional
    public WarehouseStock reserveStock(Long stockId) {
        WarehouseStock stock = warehouseStockRepository.findById(stockId)
                .orElseThrow(() -> new RuntimeException("Stock not found: " + stockId));

        if (stock.getStatus() != WarehouseStockStatus.AVAILABLE) {
            throw new RuntimeException("Stock is not available for reservation. Current status: " + stock.getStatus());
        }

        stock.setStatus(WarehouseStockStatus.RESERVED);
        return warehouseStockRepository.save(stock);
    }

    @Transactional
    public WarehouseStock dispatchStock(Long stockId) {
        WarehouseStock stock = warehouseStockRepository.findById(stockId)
                .orElseThrow(() -> new RuntimeException("Stock not found: " + stockId));

        if (stock.getStatus() != WarehouseStockStatus.RESERVED) {
            throw new RuntimeException("Stock must be RESERVED before dispatching. Current status: " + stock.getStatus());
        }

        stock.setStatus(WarehouseStockStatus.DISPATCHED);
        return warehouseStockRepository.save(stock);
    }

    public StockReport getStockReport() {
        List<WarehouseStock> allStocks = warehouseStockRepository.findAll();

        Map<String, Long> quantityByWarehouse = allStocks.stream()
                .filter(s -> s.getStatus() == WarehouseStockStatus.AVAILABLE)
                .collect(Collectors.groupingBy(
                        WarehouseStock::getWarehouseCode,
                        Collectors.summingLong(WarehouseStock::getQuantityKg)
                ));

        Long totalQuantityKg = quantityByWarehouse.values().stream().mapToLong(Long::longValue).sum();
        Integer warehouseCount = (int) quantityByWarehouse.size();

        return new StockReport(quantityByWarehouse, totalQuantityKg, warehouseCount);
    }

    public List<WarehouseStock> getStockByWarehouse(String warehouseCode) {
        return warehouseStockRepository.findByWarehouseCode(warehouseCode);
    }

    public List<WarehouseStock> getAllStock() {
        return warehouseStockRepository.findAll();
    }
}
