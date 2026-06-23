package com.digitrans.erp_service.hr;

import com.digitrans.erp_service.hr.dto.EmployeeRequest;
import com.digitrans.erp_service.hr.entity.Department;
import com.digitrans.erp_service.hr.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final EmployeeService employeeService;

    @Override
    public void run(String... args) {
        seedEmployees();
    }

    private void seedEmployees() {
        employeeService.createEmployee(new EmployeeRequest(
                "Jean", "Dupont", "jean.dupont@digitrans.com", "+33123456789",
                Department.CACAO_CAFE, "Agronomist", new BigDecimal("4200.00"), LocalDate.of(2024, 5, 10), true
        ));
        employeeService.createEmployee(new EmployeeRequest(
                "Sophie", "Martin", "sophie.martin@digitrans.com", "+33198765432",
                Department.DISTRIBUTION, "Logistics Manager", new BigDecimal("5200.00"), LocalDate.of(2023, 9, 1), true
        ));
        employeeService.createEmployee(new EmployeeRequest(
                "Ali", "Traoré", "ali.traore@digitrans.com", "+22960123456",
                Department.RESTAURATION, "Chef", new BigDecimal("3800.00"), LocalDate.of(2022, 3, 15), true
        ));
        employeeService.createEmployee(new EmployeeRequest(
                "Amélie", "Nguyen", "amelie.nguyen@digitrans.com", "+33765432109",
                Department.CACAO_CAFE, "Quality Analyst", new BigDecimal("4500.00"), LocalDate.of(2024, 1, 20), true
        ));
        employeeService.createEmployee(new EmployeeRequest(
                "Marc", "Bernard", "marc.bernard@digitrans.com", "+33122334455",
                Department.DISTRIBUTION, "Fleet Supervisor", new BigDecimal("4700.00"), LocalDate.of(2023, 6, 30), true
        ));
    }
}
