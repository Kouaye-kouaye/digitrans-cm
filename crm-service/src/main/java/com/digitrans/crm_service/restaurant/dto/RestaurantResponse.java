package com.digitrans.crm_service.restaurant.dto;

import com.digitrans.crm_service.restaurant.entity.City;

import java.time.LocalDateTime;

public record RestaurantResponse(
        Long id,
        String code,
        String name,
        String address,
        City city,
        String managerName,
        String phone,
        String email,
        Boolean active,
        Boolean offlineCapable,
        LocalDateTime lastSyncAt,
        LocalDateTime createdAt
) {}
