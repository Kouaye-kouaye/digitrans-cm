package com.digitrans.crm_service.security;

public record AuthResponse(
        String token,
        String username,
        String role,
        long expiresIn
) {}
