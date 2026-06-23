package com.digitrans.supply_service.supply.controller;

import com.digitrans.supply_service.supply.dto.ApiResponse;
import com.digitrans.supply_service.supply.dto.EventResponse;
import com.digitrans.supply_service.supply.entity.HarvestBatch;
import com.digitrans.supply_service.supply.service.HarvestService;
import com.digitrans.supply_service.supply.service.TraceabilityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supply/events")
@RequiredArgsConstructor
@Tag(name = "Supply Chain - Traçabilité Cacao/Café")
public class EventController {

    private final HarvestService harvestService;
    private final TraceabilityService traceabilityService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponse>>> getEventsByBatch(@RequestParam String batchCode) {
        HarvestBatch batch = harvestService.getByBatchCode(batchCode);
        List<EventResponse> events = traceabilityService.getEventResponses(batch);
        return ResponseEntity.ok(new ApiResponse<>(true, "Events retrieved", events));
    }
}
