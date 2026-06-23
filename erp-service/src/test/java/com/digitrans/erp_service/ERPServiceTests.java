package com.digitrans.erp_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ERPServiceTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testEmployeeCreation() throws Exception {
        String employeeJson = """
                {
                    "firstName": "Jean",
                    "lastName": "Dupont",
                    "email": "jean.dupont@digitrans.com",
                    "salaryBase": 500000,
                    "position": "Engineer"
                }
                """;

        mockMvc.perform(post("/api/erp/employees")
                .header("Authorization", "Bearer token")
                .contentType("application/json")
                .content(employeeJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Jean"))
                .andExpect(jsonPath("$.salaryBase").value(500000));
    }

    @Test
    void testEmployeeRetrieval() throws Exception {
        mockMvc.perform(get("/api/erp/employees/1")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void testPayrollProcessing() throws Exception {
        mockMvc.perform(post("/api/erp/payroll/process")
                .header("Authorization", "Bearer token")
                .param("month", "6")
                .param("year", "2026"))
                .andExpect(status().isOk());
    }

    @Test
    void testPayrollReport() throws Exception {
        mockMvc.perform(get("/api/erp/payroll/2026-06")
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
