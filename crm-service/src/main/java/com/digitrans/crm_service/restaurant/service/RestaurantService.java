package com.digitrans.crm_service.restaurant.service;

import com.digitrans.crm_service.restaurant.dto.RestaurantResponse;
import com.digitrans.crm_service.restaurant.entity.Restaurant;
import com.digitrans.crm_service.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    public RestaurantResponse createRestaurant(Restaurant restaurant) {
        Restaurant saved = restaurantRepository.save(restaurant);
        return toResponse(saved);
    }

    public RestaurantResponse getRestaurantById(Long id) {
        return restaurantRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Restaurant not found: " + id));
    }

    public List<RestaurantResponse> getAllRestaurants() {
        return restaurantRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<RestaurantResponse> getRestaurantsByCity(String city) {
        return restaurantRepository.findByCity(city).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RestaurantResponse updateRestaurant(Long id, Restaurant update) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found: " + id));

        restaurant.setName(update.getName());
        restaurant.setAddress(update.getAddress());
        restaurant.setManagerName(update.getManagerName());
        restaurant.setPhone(update.getPhone());
        restaurant.setEmail(update.getEmail());
        restaurant.setActive(update.getActive());
        restaurant.setOfflineCapable(update.getOfflineCapable());

        Restaurant saved = restaurantRepository.save(restaurant);
        return toResponse(saved);
    }

    @Transactional
    public void deleteRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found: " + id));
        restaurant.setActive(false);
        restaurantRepository.save(restaurant);
    }

    private RestaurantResponse toResponse(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getCode(),
                restaurant.getName(),
                restaurant.getAddress(),
                restaurant.getCity(),
                restaurant.getManagerName(),
                restaurant.getPhone(),
                restaurant.getEmail(),
                restaurant.getActive(),
                restaurant.getOfflineCapable(),
                restaurant.getLastSyncAt(),
                restaurant.getCreatedAt()
        );
    }
}
