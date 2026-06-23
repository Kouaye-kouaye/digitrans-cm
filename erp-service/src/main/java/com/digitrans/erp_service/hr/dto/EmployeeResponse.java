package com.digitrans.erp_service.hr.dto;

import com.digitrans.erp_service.hr.entity.Department;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        String employeeCode,
        String firstName,
        String lastName,
        String email,
        String phone,
        Department department,
        String position,
        BigDecimal baseSalary,
        LocalDate hireDate,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
