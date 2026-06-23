package com.digitrans.supply_service.supply.service;

import com.digitrans.supply_service.supply.dto.EventResponse;
import com.digitrans.supply_service.supply.dto.VerificationResult;
import com.digitrans.supply_service.supply.entity.HarvestBatch;
import com.digitrans.supply_service.supply.entity.TraceabilityEvent;
import com.digitrans.supply_service.supply.repository.TraceabilityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TraceabilityService {

    private final TraceabilityEventRepository traceabilityEventRepository;

    @Transactional
    public TraceabilityEvent addEvent(HarvestBatch batch, String eventType, String location, String operator, String metadata) {
        LocalDateTime eventAt = LocalDateTime.now();
        String hashSignature = computeHash(batch.getBatchCode(), eventType, location, eventAt);

        TraceabilityEvent event = TraceabilityEvent.builder()
                .batch(batch)
                .eventType(Enum.valueOf(com.digitrans.supply_service.supply.entity.EventType.class, eventType))
                .location(location)
                .operatorName(operator)
                .eventAt(eventAt)
                .metadata(metadata)
                .hashSignature(hashSignature)
                .build();

        return traceabilityEventRepository.save(event);
    }

    public String computeHash(String batchCode, String eventType, String location, LocalDateTime eventAt) {
        try {
            String input = batchCode + "|" + eventType + "|" + location + "|" + eventAt.truncatedTo(ChronoUnit.MILLIS).toString();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public VerificationResult verifyIntegrity(HarvestBatch batch) {
        List<TraceabilityEvent> events = traceabilityEventRepository.findByBatchIdOrderByEventAtAsc(batch.getId());

        int totalEvents = events.size();
        int validEvents = 0;

        for (TraceabilityEvent event : events) {
            String recomputedHash = computeHash(batch.getBatchCode(), event.getEventType().name(), event.getLocation(), event.getEventAt());
            if (event.getHashSignature().equals(recomputedHash)) {
                validEvents++;
            }
        }

        boolean isValid = validEvents == totalEvents && totalEvents > 0;
        String status = isValid ? "ALL_VALID" : "TAMPERED_DETECTED";
        String message = validEvents + "/" + totalEvents + " events verified";

        return new VerificationResult(isValid, status, totalEvents, validEvents, message);
    }

    public List<EventResponse> getEventResponses(HarvestBatch batch) {
        List<TraceabilityEvent> events = traceabilityEventRepository.findByBatchIdOrderByEventAtAsc(batch.getId());
        return events.stream()
                .map(e -> new EventResponse(
                        e.getId(),
                        e.getEventType(),
                        e.getLocation(),
                        e.getOperatorName(),
                        e.getEventAt(),
                        e.getMetadata(),
                        e.getHashSignature()
                ))
                .toList();
    }
}
