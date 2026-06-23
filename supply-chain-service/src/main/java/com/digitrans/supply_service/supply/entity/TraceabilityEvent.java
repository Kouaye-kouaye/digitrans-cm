package com.digitrans.supply_service.supply.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "traceability_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceabilityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private HarvestBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "location")
    private String location;

    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "event_at", nullable = false)
    private LocalDateTime eventAt;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "hash_signature")
    private String hashSignature;
}
