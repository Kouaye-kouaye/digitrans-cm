package com.digitrans.crm_service;

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
class CRMServiceTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testOrderCreation() throws Exception {
        String orderJson = """
                {
                    "customerId": 1,
                    "items": [
                        {"productId": 1, "quantity": 2},
                        {"productId": 2, "quantity": 1}
                    ],
                    "totalAmount": 45000
                }
                """;

        mockMvc.perform(post("/api/crm/orders")
                .header("Authorization", "Bearer token")
                .contentType("application/json")
                .content(orderJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(45000));
    }

    @Test
    void testOrderStatusRetrieval() throws Exception {
        mockMvc.perform(get("/api/crm/orders/1/status")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void testLoyaltyPointsAddition() throws Exception {
        mockMvc.perform(post("/api/crm/loyalty/points/add")
                .header("Authorization", "Bearer token")
                .param("customerId", "1")
                .param("points", "50"))
                .andExpect(status().isOk());
    }

    @Test
    void testCustomerHistoryRetrieval() throws Exception {
        mockMvc.perform(get("/api/crm/customers/1/history")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
