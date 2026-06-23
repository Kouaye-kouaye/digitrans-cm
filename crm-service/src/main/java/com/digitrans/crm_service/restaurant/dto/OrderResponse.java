package com.digitrans.crm_service.restaurant.dto;

import com.digitrans.crm_service.restaurant.entity.OrderMode;
import com.digitrans.crm_service.restaurant.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        Long customerId,
        String customerName,
        Long restaurantId,
        OrderStatus status,
        OrderMode orderMode,
        String offlineId,
        BigDecimal totalAmount,
        Integer loyaltyPointsEarned,
        LocalDateTime orderedAt,
        LocalDateTime deliveredAt,
        List<OrderItemResponse> items
) {}
