package com.digitrans.erp_service;

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
class ERPServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"erptest","password":"test123","role":"ADMIN"}
                        """));
        var result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"erptest","password":"test123"}
                        """));
        jwtToken = result.andReturn().getResponse().getContentAsString();
        jwtToken = jwtToken.substring(jwtToken.indexOf("\"token\":\"") + 9);
        jwtToken = jwtToken.substring(0, jwtToken.indexOf("\""));
    }

    @Test
    void testCreateEmployee() throws Exception {
        mockMvc.perform(post("/api/erp/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .content("""
                        {
                            "firstName": "Test",
                            "lastName": "User",
                            "email": "test.user@test.com",
                            "phone": "+1234567890",
                            "department": "RESTAURATION",
                            "position": "Waiter",
                            "baseSalary": 3000.00,
                            "hireDate": "2026-01-15",
                            "active": true
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeCode", startsWith("EMP-")));
    }

    @Test
    void testGetEmployeesByDepartment() throws Exception {
        mockMvc.perform(get("/api/erp/employees")
                .param("department", "RESTAURATION")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void testProcessPayroll() throws Exception {
        mockMvc.perform(post("/api/erp/payroll/process")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .content("""
                        {
                            "employeeId": 1,
                            "month": 5,
                            "year": 2026,
                            "bonuses": 500.00,
                            "deductions": 200.00
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.netSalary").exists())
                .andExpect(jsonPath("$.data.month").value(5))
                .andExpect(jsonPath("$.data.year").value(2026));
    }

    @Test
    void testDuplicatePayrollFails() throws Exception {
        mockMvc.perform(post("/api/erp/payroll/process")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .content("""
                        {
                            "employeeId": 2,
                            "month": 6,
                            "year": 2026,
                            "bonuses": 100.00,
                            "deductions": 50.00
                        }
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/erp/payroll/process")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .content("""
                        {
                            "employeeId": 2,
                            "month": 6,
                            "year": 2026,
                            "bonuses": 100.00,
                            "deductions": 50.00
                        }
                        """))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }
}
