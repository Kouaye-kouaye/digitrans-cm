package com.digitrans.supply_service.supply.controller;

import com.digitrans.supply_service.supply.dto.ApiResponse;
import com.digitrans.supply_service.supply.entity.Plantation;
import com.digitrans.supply_service.supply.repository.PlantationRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supply/plantations")
@RequiredArgsConstructor
@Tag(name = "Supply Chain - Traçabilité Cacao/Café")
public class PlantationController {

    private final PlantationRepository plantationRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Plantation>>> getAll() {
        List<Plantation> plantations = plantationRepository.findAll();
        return ResponseEntity.ok(new ApiResponse<>(true, "Plantations retrieved", plantations));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Plantation>> getById(@PathVariable Long id) {
        Plantation plantation = plantationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plantation not found: " + id));
        return ResponseEntity.ok(new ApiResponse<>(true, "Plantation retrieved", plantation));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Plantation>> create(@RequestBody Plantation plantation) {
        Plantation saved = plantationRepository.save(plantation);
        return ResponseEntity.ok(new ApiResponse<>(true, "Plantation created", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Plantation>> update(@PathVariable Long id, @RequestBody Plantation updated) {
        Plantation existing = plantationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plantation not found: " + id));
        existing.setCode(updated.getCode());
        existing.setName(updated.getName());
        existing.setRegion(updated.getRegion());
        existing.setOwnerName(updated.getOwnerName());
        existing.setProductType(updated.getProductType());
        existing.setSurfaceHectares(updated.getSurfaceHectares());
        existing.setActive(updated.getActive());
        existing.setGeoCoordinates(updated.getGeoCoordinates());
        Plantation saved = plantationRepository.save(existing);
        return ResponseEntity.ok(new ApiResponse<>(true, "Plantation updated", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        plantationRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Plantation deleted", null));
    }
}
