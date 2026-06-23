package com.digitrans.erp_service.hr.controller;

import com.digitrans.erp_service.hr.dto.ApiResponse;
import com.digitrans.erp_service.hr.dto.EmployeeRequest;
import com.digitrans.erp_service.hr.dto.EmployeeResponse;
import com.digitrans.erp_service.hr.dto.PagedResponse;
import com.digitrans.erp_service.hr.dto.PayrollRequest;
import com.digitrans.erp_service.hr.dto.PayrollResponse;
import com.digitrans.erp_service.hr.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/erp")
@RequiredArgsConstructor
@Tag(name = "ERP - Ressources Humaines")
public class ErpController {

    private final EmployeeService employeeService;

    @Operation(summary = "Create a new employee")
    @PostMapping("/employees")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = employeeService.createEmployee(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Employee created successfully", response));
    }

    @Operation(summary = "List employees by department or page")
    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<PagedResponse<EmployeeResponse>>> listEmployees(
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName"));
        var employeesPage = employeeService.getEmployeesByDepartment(department, pageable);
        PagedResponse<EmployeeResponse> pagedResponse = new PagedResponse<>(
                employeesPage.getContent(),
                page,
                size,
                employeesPage.getTotalElements(),
                employeesPage.getTotalPages()
        );
        return ResponseEntity.ok(new ApiResponse<>(true, "Employees retrieved successfully", pagedResponse));
    }

    @Operation(summary = "Search employees by name")
    @GetMapping("/employees/search")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> searchEmployees(@RequestParam String name) {
        List<EmployeeResponse> results = employeeService.searchByName(name);
        return ResponseEntity.ok(new ApiResponse<>(true, "Employee search results", results));
    }

    @Operation(summary = "Get employee by ID")
    @GetMapping("/employees/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployee(@PathVariable Long id) {
        EmployeeResponse response = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Employee retrieved successfully", response));
    }

    @Operation(summary = "Update an existing employee")
    @PutMapping("/employees/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request
    ) {
        EmployeeResponse response = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Employee updated successfully", response));
    }

    @Operation(summary = "Soft delete an employee")
    @DeleteMapping("/employees/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Employee deleted successfully", null));
    }

    @Operation(summary = "Process payroll for an employee")
    @PostMapping("/payroll/process")
    public ResponseEntity<ApiResponse<PayrollResponse>> processPayroll(
            @Valid @RequestBody PayrollRequest request
    ) {
        PayrollResponse response = employeeService.processPayroll(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payroll processed successfully", response));
    }

    @Operation(summary = "Validate a payroll record")
    @PutMapping("/payroll/{id}/validate")
    public ResponseEntity<ApiResponse<PayrollResponse>> validatePayroll(@PathVariable Long id) {
        PayrollResponse response = employeeService.validatePayroll(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payroll validated successfully", response));
    }

    @Operation(summary = "Mark a payroll record as paid")
    @PutMapping("/payroll/{id}/mark-paid")
    public ResponseEntity<ApiResponse<PayrollResponse>> markPaid(@PathVariable Long id) {
        PayrollResponse response = employeeService.markPaid(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payroll marked as paid", response));
    }

    @Operation(summary = "Get monthly payroll report")
    @GetMapping("/payroll/monthly-report")
    public ResponseEntity<ApiResponse<List<PayrollResponse>>> getMonthlyReport(
            @RequestParam int month,
            @RequestParam int year
    ) {
        List<PayrollResponse> report = employeeService.getPayrollMonthlyReport(month, year);
        return ResponseEntity.ok(new ApiResponse<>(true, "Monthly payroll report retrieved successfully", report));
    }
}
