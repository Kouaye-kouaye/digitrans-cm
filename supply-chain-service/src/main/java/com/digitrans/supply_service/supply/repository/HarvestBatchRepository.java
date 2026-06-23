package com.digitrans.supply_service.supply.repository;

import com.digitrans.supply_service.supply.entity.HarvestBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HarvestBatchRepository extends JpaRepository<HarvestBatch, Long> {
    Optional<HarvestBatch> findByBatchCode(String batchCode);
    List<HarvestBatch> findByPlantationId(Long plantationId);
    List<HarvestBatch> findByPlantationIdAndHarvestDateBetween(Long plantationId, LocalDate from, LocalDate to);
    List<HarvestBatch> findByStatus(String status);
}
