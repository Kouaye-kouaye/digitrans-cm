package com.digitrans.api_gateway.security;

public record AuthResponse(
        String token,
        String username,
        String role,
        long expiresIn
) {}
