package com.digitrans.api_gateway;

import com.digitrans.api_gateway.service.JwtService;
import com.digitrans.api_gateway.service.AuthService;
import com.digitrans.api_gateway.repository.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
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
class ApiGatewayAuthenticationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryUserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        userRepository.clear();
    }

    @Test
    void testUserRegistration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .param("username", "testuser")
                .param("password", "password123")
                .param("role", "USER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void testUserLogin() throws Exception {
        // Register first
        mockMvc.perform(post("/api/auth/register")
                .param("username", "testuser")
                .param("password", "password123")
                .param("role", "USER"));

        // Then login
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .param("username", "testuser")
                .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = result.getResponse().getContentAsString();
        assert token.contains("token");
    }

    @Test
    void testLoginWithInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .param("username", "testuser")
                .param("password", "password123")
                .param("role", "USER"));

        mockMvc.perform(post("/api/auth/login")
                .param("username", "testuser")
                .param("password", "wrongpassword"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testProtectedEndpointWithValidToken() throws Exception {
        // Register and get token
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .param("username", "testuser")
                .param("password", "password123")
                .param("role", "USER"))
                .andExpect(status().isCreated())
                .andReturn();

        String token = "test-jwt-token-here";

        // Try to access protected endpoint with token
        mockMvc.perform(get("/api/protected")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void testSwaggerUIAccess() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isOk());
    }

    @Test
    void testOpenAPIDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());
    }
}
