package com.digitrans.crm_service.restaurant.repository;

import com.digitrans.crm_service.restaurant.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhone(String phone);
    List<Customer> findByLoyaltyPointsGreaterThan(Integer points);
    
    @Query("SELECT c FROM Customer c WHERE LOWER(c.firstName) LIKE LOWER(concat('%', ?1, '%')) OR LOWER(c.lastName) LIKE LOWER(concat('%', ?1, '%'))")
    List<Customer> searchByName(String name);
}
