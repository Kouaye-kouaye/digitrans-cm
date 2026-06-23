package com.digitrans.erp_service.hr.dto;

import com.digitrans.erp_service.hr.entity.PayrollStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayrollResponse(
        Long id,
        Long employeeId,
        String employeeName,
        Integer month,
        Integer year,
        BigDecimal baseSalary,
        BigDecimal bonuses,
        BigDecimal deductions,
        BigDecimal netSalary,
        PayrollStatus status,
        LocalDateTime processedAt
) {}
