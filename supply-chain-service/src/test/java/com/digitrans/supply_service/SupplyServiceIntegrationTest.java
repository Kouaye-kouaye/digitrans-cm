package com.digitrans.supply_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SupplyServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String jwtToken;
    private String batchCode;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"supplytest","password":"test123","role":"ADMIN"}
                        """));
        var result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"supplytest","password":"test123"}
                        """));
        var body = result.andReturn().getResponse().getContentAsString();
        if (body.contains("\"token\"")) {
            jwtToken = body.substring(body.indexOf("\"token\":\"") + 9);
            jwtToken = jwtToken.substring(0, jwtToken.indexOf("\""));
        }
    }

    @Test
    @Order(1)
    void testCreateBatchAndTrace() throws Exception {
        var createResult = mockMvc.perform(post("/api/supply/batches")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .content("""
                        {
                            "plantationId": 1,
                            "harvestDate": "2026-05-21",
                            "quantityKg": 1000,
                            "qualityGrade": "A",
                            "currentLocation": "Nkolbisson"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchCode", startsWith("BATCH-")))
                .andReturn();

        var createBody = createResult.getResponse().getContentAsString();
        batchCode = createBody.substring(createBody.indexOf("\"batchCode\":\"") + 13);
        batchCode = batchCode.substring(0, batchCode.indexOf("\""));

        mockMvc.perform(put("/api/supply/batches/{batchCode}/status", batchCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .content("""
                        {"status":"IN_TRANSIT","location":"En route","operatorName":"Operator"}
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/supply/batches/{batchCode}/status", batchCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .content("""
                        {"status":"AT_WAREHOUSE","location":"Entrepôt","operatorName":"Operator"}
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/supply/batches/{batchCode}/status", batchCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .content("""
                        {"status":"PROCESSED","location":"Usine","operatorName":"Operator"}
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/supply/batches/{batchCode}/trace", batchCode)
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.events.length()").value(4));
    }

    @Test
    @Order(2)
    void testIntegrityVerification() throws Exception {
        mockMvc.perform(get("/api/supply/batches/{batchCode}/verify", batchCode)
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ALL_VALID"))
                .andExpect(jsonPath("$.data.isValid").value(true));
    }

    @Test
    @Order(3)
    void testTamperingDetection() throws Exception {
        jdbcTemplate.update(
                "UPDATE traceability_events SET hash_signature = 'corrupted' WHERE batch_id = (SELECT id FROM harvest_batches WHERE batch_code = ?)",
                batchCode);

        mockMvc.perform(get("/api/supply/batches/{batchCode}/verify", batchCode)
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("TAMPERED_DETECTED"))
                .andExpect(jsonPath("$.data.isValid").value(false));
    }
}
