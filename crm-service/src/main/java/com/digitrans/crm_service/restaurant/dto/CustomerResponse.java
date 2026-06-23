package com.digitrans.crm_service.restaurant.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        String customerCode,
        String firstName,
        String lastName,
        String phone,
        String email,
        String city,
        Integer loyaltyPoints,
        Integer totalOrders,
        BigDecimal totalSpent,
        LocalDateTime lastVisit,
        LocalDateTime createdAt
) {}
