package com.digitrans.supply_service.supply.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "harvest_batches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HarvestBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_code", unique = true, nullable = false)
    private String batchCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plantation_id", nullable = false)
    private Plantation plantation;

    @Column(name = "harvest_date", nullable = false)
    private LocalDate harvestDate;

    @Column(name = "quantity_kg", nullable = false)
    private Integer quantityKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_grade", nullable = false)
    private QualityGrade qualityGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BatchStatus status;

    @Column(name = "current_location")
    private String currentLocation;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "blockchain_tx_hash")
    private String blockchainTxHash;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
