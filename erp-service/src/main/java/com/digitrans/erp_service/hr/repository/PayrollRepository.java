package com.digitrans.erp_service.hr.repository;

import com.digitrans.erp_service.hr.entity.PayrollRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<PayrollRecord, Long> {
    List<PayrollRecord> findByPayrollMonthAndPayrollYearOrderByEmployee(int payrollMonth, int payrollYear);
    Optional<PayrollRecord> findByEmployeeIdAndPayrollMonthAndPayrollYear(Long employeeId, int payrollMonth, int payrollYear);
}
