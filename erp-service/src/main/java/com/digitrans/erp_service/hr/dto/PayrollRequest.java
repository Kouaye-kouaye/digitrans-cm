package com.digitrans.erp_service.hr.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PayrollRequest(
        @NotNull(message = "employeeId cannot be null")
        Long employeeId,

        @NotNull(message = "month cannot be null")
        @Min(value = 1, message = "month must be between 1 and 12")
        Integer month,

        @NotNull(message = "year cannot be null")
        @Min(value = 1900, message = "year must be valid")
        Integer year,

        @NotNull(message = "bonuses cannot be null")
        BigDecimal bonuses,

        @NotNull(message = "deductions cannot be null")
        BigDecimal deductions
) {}
