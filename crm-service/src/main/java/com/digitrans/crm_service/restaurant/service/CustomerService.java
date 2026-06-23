package com.digitrans.crm_service.restaurant.service;

import com.digitrans.crm_service.restaurant.dto.CustomerResponse;
import com.digitrans.crm_service.restaurant.entity.Customer;
import com.digitrans.crm_service.restaurant.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse createOrFindByPhone(String phone, String firstName, String lastName, String city) {
        var existing = customerRepository.findByPhone(phone);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        long sequence = customerRepository.count() + 1;
        String code = String.format("CUST-%06d", sequence);

        Customer customer = Customer.builder()
                .customerCode(code)
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .city(city)
                .build();

        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    @Transactional
    public void addLoyaltyPoints(Long customerId, BigDecimal orderAmount) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

        // 1 point per 500 FCFA
        int pointsEarned = orderAmount.divide(new BigDecimal(500)).intValue();
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + pointsEarned);

        customerRepository.save(customer);
    }

    public List<CustomerResponse> getTopCustomers(Long restaurantId, int limit) {
        var page = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "loyaltyPoints"));
        return customerRepository.findByLoyaltyPointsGreaterThan(0).stream()
                .limit(limit)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<CustomerResponse> searchByName(String name) {
        return customerRepository.searchByName(name).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CustomerResponse getCustomerId(Long id) {
        return customerRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getCity(),
                customer.getLoyaltyPoints(),
                customer.getTotalOrders(),
                customer.getTotalSpent(),
                customer.getLastVisit(),
                customer.getCreatedAt()
        );
    }
}
