package com.digitrans.supply_service.supply.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "warehouse_stock")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_code")
    private String warehouseCode;

    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "location")
    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private HarvestBatch batch;

    @Column(name = "quantity_kg", nullable = false)
    private Integer quantityKg;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WarehouseStockStatus status;
}
