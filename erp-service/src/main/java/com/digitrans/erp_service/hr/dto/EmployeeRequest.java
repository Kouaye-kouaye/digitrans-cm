package com.digitrans.erp_service.hr.dto;

import com.digitrans.erp_service.hr.entity.Department;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeRequest(
        @NotBlank(message = "First name cannot be blank")
        String firstName,

        @NotBlank(message = "Last name cannot be blank")
        String lastName,

        @Email(message = "Email must be valid")
        @NotBlank(message = "Email cannot be blank")
        String email,

        String phone,

        @NotNull(message = "Department cannot be null")
        Department department,

        @NotBlank(message = "Position cannot be blank")
        String position,

        @NotNull(message = "Base salary cannot be null")
        BigDecimal baseSalary,

        @NotNull(message = "Hire date cannot be null")
        LocalDate hireDate,

        Boolean active
) {}
