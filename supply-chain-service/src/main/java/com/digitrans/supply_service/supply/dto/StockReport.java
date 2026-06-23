package com.digitrans.supply_service.supply.dto;

import java.util.Map;

public record StockReport(
        Map<String, Long> quantityByWarehouse,
        Long totalQuantityKg,
        Integer warehouseCount
) {}
