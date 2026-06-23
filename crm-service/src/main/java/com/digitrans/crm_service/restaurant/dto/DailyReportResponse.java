package com.digitrans.crm_service.restaurant.dto;

import java.math.BigDecimal;

public record DailyReportResponse(
        int totalOrders,
        BigDecimal totalRevenue,
        BigDecimal averageTicket
) {}
