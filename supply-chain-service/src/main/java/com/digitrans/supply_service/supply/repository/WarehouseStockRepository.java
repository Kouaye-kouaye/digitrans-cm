package com.digitrans.supply_service.supply.repository;

import com.digitrans.supply_service.supply.entity.WarehouseStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Long> {
    List<WarehouseStock> findByWarehouseCode(String warehouseCode);
    List<WarehouseStock> findByBatchId(Long batchId);
}
