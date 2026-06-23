package com.digitrans.crm_service.restaurant.repository;

import com.digitrans.crm_service.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    List<Restaurant> findByCity(String city);
    List<Restaurant> findByActiveTrue();
}
