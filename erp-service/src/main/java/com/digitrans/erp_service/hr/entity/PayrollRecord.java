package com.digitrans.erp_service.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll_records", uniqueConstraints = {
        @UniqueConstraint(name = "uc_payroll_record_employee_month_year", columnNames = {"employee_id", "payroll_month", "payroll_year"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "payroll_month", nullable = false)
    private Integer payrollMonth;

    @Column(name = "payroll_year", nullable = false)
    private Integer payrollYear;

    @Column(name = "base_salary", precision = 15, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "bonuses", precision = 15, scale = 2)
    private BigDecimal bonuses;

    @Column(name = "deductions", precision = 15, scale = 2)
    private BigDecimal deductions;

    @Column(name = "net_salary", precision = 15, scale = 2)
    private BigDecimal netSalary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PayrollStatus status;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
