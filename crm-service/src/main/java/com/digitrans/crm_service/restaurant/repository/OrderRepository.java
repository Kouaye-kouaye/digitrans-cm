package com.digitrans.crm_service.restaurant.repository;

import com.digitrans.crm_service.restaurant.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByRestaurantIdAndOrderedAtBetween(Long restaurantId, LocalDateTime from, LocalDateTime to);
    List<Order> findByStatusAndRestaurantId(String status, Long restaurantId);
    Optional<Order> findByOfflineId(String offlineId);
    
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.restaurant.id = ?1 AND o.orderedAt BETWEEN ?2 AND ?3")
    BigDecimal sumTotalAmountByRestaurantAndDateRange(Long restaurantId, LocalDateTime from, LocalDateTime to);
}
