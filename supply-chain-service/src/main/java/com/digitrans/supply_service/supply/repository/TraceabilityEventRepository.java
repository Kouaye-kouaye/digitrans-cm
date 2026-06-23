package com.digitrans.supply_service.supply.repository;

import com.digitrans.supply_service.supply.entity.TraceabilityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TraceabilityEventRepository extends JpaRepository<TraceabilityEvent, Long> {
    List<TraceabilityEvent> findByBatchIdOrderByEventAtAsc(Long batchId);
}
