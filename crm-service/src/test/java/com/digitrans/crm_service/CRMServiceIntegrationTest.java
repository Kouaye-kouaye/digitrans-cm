package com.digitrans.crm_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CRMServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"crmtest","password":"test123","role":"ADMIN"}
                        """));
        var result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"crmtest","password":"test123"}
                        """));
        var body = result.andReturn().getResponse().getContentAsString();
        jwtToken = body.substring(body.indexOf("\"token\":\"") + 9);
        jwtToken = jwtToken.substring(0, jwtToken.indexOf("\""));
    }

    @Test
    void testCreateOrder() throws Exception {
        mockMvc.perform(post("/api/crm/orders")
                .param("restaurantId", "1")
                .param("customerId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .content("""
                        [
                            {"menuItemId": 1, "quantity": 2},
                            {"menuItemId": 3, "quantity": 1}
                        ]
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber", startsWith("ORD-")));
    }

    @Test
    void testOfflineSync() throws Exception {
        mockMvc.perform(post("/api/crm/orders/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .content("""
                        [
                            {
                                "offlineId": "off-1",
                                "restaurantId": 1,
                                "customerId": 1,
                                "items": [{"menuItemId": 1, "quantity": 2}],
                                "totalAmount": 7000,
                                "orderedAt": "2026-05-21T10:00:00"
                            },
                            {
                                "offlineId": "off-2",
                                "restaurantId": 1,
                                "customerId": 2,
                                "items": [{"menuItemId": 5, "quantity": 3}],
                                "totalAmount": 1500,
                                "orderedAt": "2026-05-21T11:00:00"
                            },
                            {
                                "offlineId": "off-1",
                                "restaurantId": 1,
                                "customerId": 1,
                                "items": [{"menuItemId": 1, "quantity": 2}],
                                "totalAmount": 7000,
                                "orderedAt": "2026-05-21T10:00:00"
                            }
                        ]
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.created").value(2))
                .andExpect(jsonPath("$.data.skipped").value(1))
                .andExpect(jsonPath("$.data.errors").isArray());
    }

    @Test
    void testLoyaltyPoints() throws Exception {
        mockMvc.perform(post("/api/crm/orders")
                .param("restaurantId", "1")
                .param("customerId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .content("""
                        [
                            {"menuItemId": 1, "quantity": 2},
                            {"menuItemId": 3, "quantity": 1}
                        ]
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/crm/customers/1")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loyaltyPoints").value(greaterThan(150)));
    }

    @Test
    void testDailyReport() throws Exception {
        java.time.LocalDate today = java.time.LocalDate.now();
        mockMvc.perform(get("/api/crm/orders/restaurant/1/daily")
                .param("date", today.toString())
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOrders").value(greaterThan(0)));
    }
}
