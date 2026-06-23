package com.digitrans.supply_service.supply.repository;

import com.digitrans.supply_service.supply.entity.Plantation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlantationRepository extends JpaRepository<Plantation, Long> {
    List<Plantation> findByActiveTrue();
    List<Plantation> findByProductType(String productType);
    List<Plantation> findByRegion(String region);
}
