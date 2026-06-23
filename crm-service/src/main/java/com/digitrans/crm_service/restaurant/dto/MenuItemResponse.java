package com.digitrans.crm_service.restaurant.dto;

import com.digitrans.crm_service.restaurant.entity.MenuItem_Category;

import java.math.BigDecimal;

public record MenuItemResponse(
        Long id,
        String code,
        String name,
        String description,
        MenuItem_Category category,
        BigDecimal price,
        Boolean available,
        Long restaurantId
) {}
