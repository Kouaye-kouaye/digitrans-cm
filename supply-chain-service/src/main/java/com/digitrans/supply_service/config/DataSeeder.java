package com.digitrans.supply_service.config;

import com.digitrans.supply_service.supply.entity.*;
import com.digitrans.supply_service.supply.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final PlantationRepository plantationRepository;
    private final HarvestBatchRepository harvestBatchRepository;
    private final TraceabilityEventRepository traceabilityEventRepository;
    private final WarehouseStockRepository warehouseStockRepository;

    @Override
    public void run(String... args) {
        if (plantationRepository.count() > 0) {
            log.info("Data already seeded, skipping.");
            return;
        }

        Plantation plt1 = plantationRepository.save(Plantation.builder()
                .code("PLT-001")
                .name("Plantation de Nkolbisson")
                .region("Centre")
                .ownerName("Jean Mbarga")
                .productType(ProductType.CACAO)
                .surfaceHectares(25.5)
                .active(true)
                .geoCoordinates("3.8667,11.5167")
                .build());

        Plantation plt2 = plantationRepository.save(Plantation.builder()
                .code("PLT-002")
                .name("Plantation de Bafoussam")
                .region("Ouest")
                .ownerName("Marie Kamdem")
                .productType(ProductType.CAFE)
                .surfaceHectares(18.0)
                .active(true)
                .geoCoordinates("5.4667,10.4167")
                .build());

        HarvestBatch batch1 = harvestBatchRepository.save(HarvestBatch.builder()
                .batchCode("BATCH-2026-001")
                .plantation(plt1)
                .harvestDate(LocalDate.of(2026, 3, 10))
                .quantityKg(2000)
                .qualityGrade(QualityGrade.A)
                .status(BatchStatus.AT_WAREHOUSE)
                .currentLocation("Entrepôt de Douala")
                .blockchainTxHash("0x" + "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2")
                .build());

        HarvestBatch batch2 = harvestBatchRepository.save(HarvestBatch.builder()
                .batchCode("BATCH-2026-002")
                .plantation(plt1)
                .harvestDate(LocalDate.of(2026, 4, 5))
                .quantityKg(1500)
                .qualityGrade(QualityGrade.B)
                .status(BatchStatus.IN_TRANSIT)
                .currentLocation("En transit vers Yaoundé")
                .blockchainTxHash("0x" + "b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3")
                .build());

        HarvestBatch batch3 = harvestBatchRepository.save(HarvestBatch.builder()
                .batchCode("BATCH-2026-003")
                .plantation(plt2)
                .harvestDate(LocalDate.of(2026, 4, 20))
                .quantityKg(3000)
                .qualityGrade(QualityGrade.A)
                .status(BatchStatus.PROCESSED)
                .currentLocation("Unité de transformation de Douala")
                .blockchainTxHash("0x" + "c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4")
                .build());

        seedEvents(batch1, batch2, batch3);

        seedWarehouseStock(batch1, batch3);

        log.info("Seed data inserted: 2 plantations, 3 batches, 12 events, 2 warehouse stocks.");
    }

    private void seedEvents(HarvestBatch b1, HarvestBatch b2, HarvestBatch b3) {
        traceabilityEventRepository.save(TraceabilityEvent.builder()
                .batch(b1).eventType(EventType.HARVEST).location("Nkolbisson")
                .operatorName("Jean Mbarga").eventAt(LocalDateTime.of(2026, 3, 10, 8, 0))
                .hashSignature("hash_b1_harvest").build());
        traceabilityEventRepository.save(TraceabilityEvent.builder()
                .batch(b1).eventType(EventType.QUALITY_CHECK).location("Nkolbisson")
                .operatorName("Agent qualité").eventAt(LocalDateTime.of(2026, 3, 10, 14, 0))
                .metadata("{\"grade\":\"A\",\"humidity\":\"6.5%\"}")
                .hashSignature("hash_b1_qc").build());
        traceabilityEventRepository.save(TraceabilityEvent.builder()
                .batch(b1).eventType(EventType.TRANSPORT_START).location("Nkolbisson")
                .operatorName("Transport Logistique").eventAt(LocalDateTime.of(2026, 3, 11, 6, 0))
                .hashSignature("hash_b1_transport").build());
        traceabilityEventRepository.save(TraceabilityEvent.builder()
                .batch(b1).eventType(EventType.WAREHOUSE_ARRIVAL).location("Entrepôt de Douala")
                .operatorName("Gestionnaire entrepôt").eventAt(LocalDateTime.of(2026, 3, 12, 10, 0))
                .hashSignature("hash_b1_warehouse").build());

        traceabilityEventRepository.save(TraceabilityEvent.builder()
                .batch(b2).eventType(EventType.HARVEST).location("Nkolbisson")
                .operatorName("Jean Mbarga").eventAt(LocalDateTime.of(2026, 4, 5, 7, 30))
                .hashSignature("hash_b2_harvest").build());
        traceabilityEventRepository.save(TraceabilityEvent.builder()
                .batch(b2).eventType(EventType.QUALITY_CHECK).location("Nkolbisson")
                .operatorName("Agent qualité").eventAt(LocalDateTime.of(2026, 4, 5, 13, 0))
                .metadata("{\"grade\":\"B\",\"humidity\":\"7.2%\"}")
                .hashSignature("hash_b2_qc").build());
        traceabilityEventRepository.save(TraceabilityEvent.builder()
                .batch(b2).eventType(EventType.TRANSPORT_START).location("Nkolbisson")
                .operatorName("Transport Logistique").eventAt(LocalDateTime.of(2026, 4, 6, 5, 0))
                .hashSignature("hash_b2_transport").build());
        traceabilityEventRepository.save(TraceabilityEvent.builder()
                .batch(b2).eventType(EventType.WAREHOUSE_ARRIVAL).location("Entrepôt de Yaoundé")
                .operatorName("Gestionnaire entrepôt").eventAt(LocalDateTime.of(2026, 4, 7, 9, 0))
                .hashSignature("hash_b2_warehouse").build());

        traceabilityEventRepository.save(TraceabilityEvent.builder()
                .batch(b3).eventType(EventType.HARVEST).location("Bafoussam")
                .operatorName("Marie Kamdem").eventAt(LocalDateTime.of(2026, 4, 20, 8, 0))
                .hashSignature("hash_b3_harvest").build());
        traceabilityEventRepository.save(TraceabilityEvent.builder()
                .batch(b3).eventType(EventType.QUALITY_CHECK).location("Bafoussam")
                .operatorName("Agent qualité").eventAt(LocalDateTime.of(2026, 4, 20, 15, 0))
                .metadata("{\"grade\":\"A\",\"humidity\":\"5.8%\"}")
                .hashSignature("hash_b3_qc").build());
        traceabilityEventRepository.save(TraceabilityEvent.builder()
                .batch(b3).eventType(EventType.TRANSPORT_START).location("Bafoussam")
                .operatorName("Transport Logistique").eventAt(LocalDateTime.of(2026, 4, 21, 4, 0))
                .hashSignature("hash_b3_transport").build());
        traceabilityEventRepository.save(TraceabilityEvent.builder()
                .batch(b3).eventType(EventType.PROCESSING).location("Unité de transformation de Douala")
                .operatorName("Chef d'unité").eventAt(LocalDateTime.of(2026, 4, 24, 11, 0))
                .metadata("{\"process\":\"fermentation\",\"duration\":\"48h\"}")
                .hashSignature("hash_b3_processing").build());
    }

    private void seedWarehouseStock(HarvestBatch b1, HarvestBatch b3) {
        warehouseStockRepository.save(WarehouseStock.builder()
                .warehouseCode("WH-DLA-01")
                .warehouseName("Entrepôt de Douala")
                .location("Douala, Zone Portuaire")
                .batch(b1)
                .quantityKg(2000)
                .arrivalDate(LocalDate.of(2026, 3, 12))
                .expiryDate(LocalDate.of(2026, 9, 12))
                .status(WarehouseStockStatus.AVAILABLE)
                .build());

        warehouseStockRepository.save(WarehouseStock.builder()
                .warehouseCode("WH-YDE-01")
                .warehouseName("Entrepôt de Yaoundé")
                .location("Yaoundé, Mvan")
                .batch(b3)
                .quantityKg(1500)
                .arrivalDate(LocalDate.of(2026, 4, 24))
                .expiryDate(LocalDate.of(2026, 10, 24))
                .status(WarehouseStockStatus.AVAILABLE)
                .build());
    }
}
