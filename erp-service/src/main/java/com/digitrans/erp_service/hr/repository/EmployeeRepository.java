package com.digitrans.erp_service.hr.repository;

import com.digitrans.erp_service.hr.entity.Department;
import com.digitrans.erp_service.hr.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByDepartmentAndActiveTrue(Department department);
    Page<Employee> findByDepartmentAndActiveTrue(Department department, Pageable pageable);
    Page<Employee> findByActiveTrue(Pageable pageable);
    List<Employee> findByFirstNameContainingIgnoreCaseAndActiveTrue(String firstName);
    List<Employee> findByLastNameContainingIgnoreCaseAndActiveTrue(String lastName);
    boolean existsByEmployeeCode(String employeeCode);
    Optional<Employee> findByEmail(String email);
}
