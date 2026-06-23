package com.digitrans.supply_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SupplyChainServiceTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testPlantationCreation() throws Exception {
        String plantationJson = """
                {
                    "code": "PLT001",
                    "name": "Cacao Plantation Douala",
                    "region": "Littoral",
                    "ownerName": "Farmer Smith",
                    "productType": "CACAO",
                    "surfaceHectares": 50.5
                }
                """;

        mockMvc.perform(post("/api/supply/plantations")
                .header("Authorization", "Bearer token")
                .contentType("application/json")
                .content(plantationJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PLT001"))
                .andExpect(jsonPath("$.productType").value("CACAO"));
    }

    @Test
    void testBatchCreation() throws Exception {
        String batchJson = """
                {
                    "plantationId": 1,
                    "harvestDate": "2026-06-15",
                    "quantityKg": 1000,
                    "qualityGrade": "A",
                    "currentLocation": "Plantation Zone 1"
                }
                """;

        mockMvc.perform(post("/api/supply/batches")
                .header("Authorization", "Bearer token")
                .contentType("application/json")
                .content(batchJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantityKg").value(1000))
                .andExpect(jsonPath("$.qualityGrade").value("A"));
    }

    @Test
    void testBatchTraceabilityRetrieval() throws Exception {
        mockMvc.perform(get("/api/supply/batches/BATCH-2026-001/trace")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void testBatchIntegrityVerification() throws Exception {
        mockMvc.perform(get("/api/supply/batches/BATCH-2026-001/verify")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").isBoolean());
    }

    @Test
    void testBatchStatusUpdate() throws Exception {
        mockMvc.perform(put("/api/supply/batches/BATCH-2026-001/status")
                .header("Authorization", "Bearer token")
                .param("status", "AT_WAREHOUSE"))
                .andExpect(status().isOk());
    }

    @Test
    void testWarehouseStockReport() throws Exception {
        mockMvc.perform(get("/api/supply/warehouse/stock")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testMetricsExposure() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }
}
