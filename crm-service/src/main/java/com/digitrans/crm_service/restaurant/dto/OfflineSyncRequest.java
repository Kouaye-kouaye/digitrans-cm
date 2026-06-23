package com.digitrans.crm_service.restaurant.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OfflineSyncRequest(
        String offlineId,
        Long restaurantId,
        Long customerId,
        List<ItemRequest> items,
        BigDecimal totalAmount,
        LocalDateTime orderedAt
) {}
