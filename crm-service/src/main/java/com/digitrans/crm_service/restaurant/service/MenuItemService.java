package com.digitrans.crm_service.restaurant.service;

import com.digitrans.crm_service.restaurant.dto.MenuItemResponse;
import com.digitrans.crm_service.restaurant.entity.MenuItem;
import com.digitrans.crm_service.restaurant.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;

    public MenuItemResponse createMenuItem(MenuItem menuItem) {
        MenuItem saved = menuItemRepository.save(menuItem);
        return toResponse(saved);
    }

    public MenuItemResponse getMenuItemById(Long id) {
        return menuItemRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Menu item not found: " + id));
    }

    public List<MenuItemResponse> getAvailableMenuItems(Long restaurantId) {
        return menuItemRepository.findByRestaurantIdAndAvailableTrue(restaurantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public MenuItemResponse updateMenuItem(Long id, MenuItem update) {
        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found: " + id));

        menuItem.setName(update.getName());
        menuItem.setDescription(update.getDescription());
        menuItem.setCategory(update.getCategory());
        menuItem.setPrice(update.getPrice());
        menuItem.setAvailable(update.getAvailable());

        MenuItem saved = menuItemRepository.save(menuItem);
        return toResponse(saved);
    }

    public void deleteMenuItem(Long id) {
        menuItemRepository.deleteById(id);
    }

    private MenuItemResponse toResponse(MenuItem menuItem) {
        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getCode(),
                menuItem.getName(),
                menuItem.getDescription(),
                menuItem.getCategory(),
                menuItem.getPrice(),
                menuItem.getAvailable(),
                menuItem.getRestaurant().getId()
        );
    }
}
