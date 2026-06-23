package com.digitrans.erp_service.hr.service;

import com.digitrans.erp_service.hr.dto.EmployeeRequest;
import com.digitrans.erp_service.hr.dto.EmployeeResponse;
import com.digitrans.erp_service.hr.dto.PayrollRequest;
import com.digitrans.erp_service.hr.dto.PayrollResponse;
import com.digitrans.erp_service.hr.entity.Department;
import com.digitrans.erp_service.hr.entity.Employee;
import com.digitrans.erp_service.hr.entity.PayrollRecord;
import com.digitrans.erp_service.hr.entity.PayrollStatus;
import com.digitrans.erp_service.hr.repository.EmployeeRepository;
import com.digitrans.erp_service.hr.repository.PayrollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PayrollRepository payrollRepository;

    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email already used: " + request.email());
        }

        String employeeCode = generateEmployeeCode();
        Employee employee = Employee.builder()
                .employeeCode(employeeCode)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .department(request.department())
                .position(request.position())
                .baseSalary(request.baseSalary())
                .hireDate(request.hireDate())
                .active(request.active() == null ? true : request.active())
                .build();

        Employee saved = employeeRepository.save(employee);
        return toResponse(saved);
    }

    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + id));

        if (!Objects.equals(employee.getEmail(), request.email()) && employeeRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email already used: " + request.email());
        }

        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setPhone(request.phone());
        employee.setDepartment(request.department());
        employee.setPosition(request.position());
        employee.setBaseSalary(request.baseSalary());
        employee.setHireDate(request.hireDate());
        employee.setActive(request.active() == null ? employee.getActive() : request.active());

        Employee saved = employeeRepository.save(employee);
        return toResponse(saved);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + id));
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    public EmployeeResponse getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + id));
    }

    public Page<EmployeeResponse> getEmployeesByDepartment(String department, Pageable pageable) {
        Page<Employee> page;
        if (department == null || department.isBlank()) {
            page = employeeRepository.findByActiveTrue(pageable);
        } else {
            Department dept = Department.valueOf(department);
            page = employeeRepository.findByDepartmentAndActiveTrue(dept, pageable);
        }
        return page.map(this::toResponse);
    }

    public List<EmployeeResponse> searchByName(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }

        Set<Employee> results = new LinkedHashSet<>();
        results.addAll(employeeRepository.findByFirstNameContainingIgnoreCaseAndActiveTrue(name));
        results.addAll(employeeRepository.findByLastNameContainingIgnoreCaseAndActiveTrue(name));

        return results.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PayrollResponse processPayroll(PayrollRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found: " + request.employeeId()));

        payrollRepository.findByEmployeeIdAndPayrollMonthAndPayrollYear(request.employeeId(), request.month(), request.year())
                .ifPresent(record -> {
                    throw new RuntimeException("Payroll record already exists for employee " + request.employeeId() + " for month " + request.month() + "/" + request.year());
                });

        BigDecimal netSalary = calculateNetSalary(employee.getBaseSalary(), request.bonuses(), request.deductions());

        PayrollRecord payroll = PayrollRecord.builder()
                .employee(employee)
                .payrollMonth(request.month())
                .payrollYear(request.year())
                .baseSalary(employee.getBaseSalary())
                .bonuses(request.bonuses())
                .deductions(request.deductions())
                .netSalary(netSalary)
                .status(PayrollStatus.DRAFT)
                .processedAt(LocalDateTime.now())
                .build();

        PayrollRecord saved = payrollRepository.save(payroll);
        return toPayrollResponse(saved);
    }

    public PayrollResponse validatePayroll(Long id) {
        PayrollRecord payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll record not found: " + id));
        if (payroll.getStatus() != PayrollStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT payroll records can be validated");
        }
        payroll.setStatus(PayrollStatus.VALIDATED);
        PayrollRecord saved = payrollRepository.save(payroll);
        return toPayrollResponse(saved);
    }

    public PayrollResponse markPaid(Long id) {
        PayrollRecord payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll record not found: " + id));
        if (payroll.getStatus() != PayrollStatus.VALIDATED) {
            throw new RuntimeException("Only VALIDATED payroll records can be marked as PAID");
        }
        payroll.setStatus(PayrollStatus.PAID);
        PayrollRecord saved = payrollRepository.save(payroll);
        return toPayrollResponse(saved);
    }

    public List<PayrollResponse> getPayrollMonthlyReport(int month, int year) {
        return payrollRepository.findByPayrollMonthAndPayrollYearOrderByEmployee(month, year).stream()
                .map(this::toPayrollResponse)
                .collect(Collectors.toList());
    }

    private String generateEmployeeCode() {
        long sequence = employeeRepository.count() + 1;
        String code = formatEmployeeCode(sequence);
        while (employeeRepository.existsByEmployeeCode(code)) {
            sequence++;
            code = formatEmployeeCode(sequence);
        }
        return code;
    }

    private String formatEmployeeCode(long sequence) {
        return String.format("EMP-%03d", sequence);
    }

    private BigDecimal calculateNetSalary(BigDecimal baseSalary, BigDecimal bonuses, BigDecimal deductions) {
        return baseSalary.add(bonuses).subtract(deductions);
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getDepartment(),
                employee.getPosition(),
                employee.getBaseSalary(),
                employee.getHireDate(),
                employee.getActive(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }

    private PayrollResponse toPayrollResponse(PayrollRecord payroll) {
        return new PayrollResponse(
                payroll.getId(),
                payroll.getEmployee().getId(),
                payroll.getEmployee().getFirstName() + " " + payroll.getEmployee().getLastName(),
                payroll.getPayrollMonth(),
                payroll.getPayrollYear(),
                payroll.getBaseSalary(),
                payroll.getBonuses(),
                payroll.getDeductions(),
                payroll.getNetSalary(),
                payroll.getStatus(),
                payroll.getProcessedAt()
        );
    }
}
