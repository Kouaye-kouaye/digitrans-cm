package com.digitrans.crm_service.restaurant.controller;

import com.digitrans.crm_service.restaurant.dto.*;
import com.digitrans.crm_service.restaurant.entity.*;
import com.digitrans.crm_service.restaurant.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/crm")
@RequiredArgsConstructor
@Tag(name = "CRM - SavoirManger Restaurants")
public class CrmController {

    private final RestaurantService restaurantService;
    private final CustomerService customerService;
    private final MenuItemService menuItemService;
    private final OrderService orderService;

    // ============ RESTAURANTS ============

    @Operation(summary = "Create a new restaurant")
    @PostMapping("/restaurants")
    public ResponseEntity<ApiResponse<RestaurantResponse>> createRestaurant(@RequestBody Restaurant restaurant) {
        RestaurantResponse response = restaurantService.createRestaurant(restaurant);
        return ResponseEntity.ok(new ApiResponse<>(true, "Restaurant created successfully", response));
    }

    @Operation(summary = "Get all restaurants")
    @GetMapping("/restaurants")
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getAllRestaurants() {
        List<RestaurantResponse> restaurants = restaurantService.getAllRestaurants();
        return ResponseEntity.ok(new ApiResponse<>(true, "Restaurants retrieved successfully", restaurants));
    }

    @Operation(summary = "Get restaurants by city")
    @GetMapping("/restaurants/by-city/{city}")
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getRestaurantsByCity(@PathVariable String city) {
        List<RestaurantResponse> restaurants = restaurantService.getRestaurantsByCity(city);
        return ResponseEntity.ok(new ApiResponse<>(true, "Restaurants retrieved successfully", restaurants));
    }

    @Operation(summary = "Get restaurant by ID")
    @GetMapping("/restaurants/{id}")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getRestaurantById(@PathVariable Long id) {
        RestaurantResponse response = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Restaurant retrieved successfully", response));
    }

    @Operation(summary = "Update restaurant")
    @PutMapping("/restaurants/{id}")
    public ResponseEntity<ApiResponse<RestaurantResponse>> updateRestaurant(
            @PathVariable Long id,
            @RequestBody Restaurant restaurant
    ) {
        RestaurantResponse response = restaurantService.updateRestaurant(id, restaurant);
        return ResponseEntity.ok(new ApiResponse<>(true, "Restaurant updated successfully", response));
    }

    @Operation(summary = "Delete restaurant")
    @DeleteMapping("/restaurants/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRestaurant(@PathVariable Long id) {
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Restaurant deleted successfully", null));
    }

    // ============ CUSTOMERS ============

    @Operation(summary = "Search customers by name")
    @GetMapping("/customers/search")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> searchCustomers(@RequestParam String name) {
        List<CustomerResponse> customers = customerService.searchByName(name);
        return ResponseEntity.ok(new ApiResponse<>(true, "Customers retrieved successfully", customers));
    }

    @Operation(summary = "Get top customers by loyalty points")
    @GetMapping("/customers/top/{restaurantId}")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getTopCustomers(
            @PathVariable Long restaurantId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<CustomerResponse> customers = customerService.getTopCustomers(restaurantId, limit);
        return ResponseEntity.ok(new ApiResponse<>(true, "Top customers retrieved successfully", customers));
    }

    @Operation(summary = "Get customer by ID")
    @GetMapping("/customers/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable Long id) {
        CustomerResponse response = customerService.getCustomerId(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Customer retrieved successfully", response));
    }

    // ============ MENU ITEMS ============

    @Operation(summary = "Create menu item")
    @PostMapping("/menu-items")
    public ResponseEntity<ApiResponse<MenuItemResponse>> createMenuItem(@RequestBody MenuItem menuItem) {
        MenuItemResponse response = menuItemService.createMenuItem(menuItem);
        return ResponseEntity.ok(new ApiResponse<>(true, "Menu item created successfully", response));
    }

    @Operation(summary = "Get available menu items by restaurant")
    @GetMapping("/menu-items/available/{restaurantId}")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getAvailableMenuItems(@PathVariable Long restaurantId) {
        List<MenuItemResponse> items = menuItemService.getAvailableMenuItems(restaurantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Menu items retrieved successfully", items));
    }

    @Operation(summary = "Get menu item by ID")
    @GetMapping("/menu-items/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getMenuItemById(@PathVariable Long id) {
        MenuItemResponse response = menuItemService.getMenuItemById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Menu item retrieved successfully", response));
    }

    @Operation(summary = "Update menu item")
    @PutMapping("/menu-items/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateMenuItem(
            @PathVariable Long id,
            @RequestBody MenuItem menuItem
    ) {
        MenuItemResponse response = menuItemService.updateMenuItem(id, menuItem);
        return ResponseEntity.ok(new ApiResponse<>(true, "Menu item updated successfully", response));
    }

    @Operation(summary = "Delete menu item")
    @DeleteMapping("/menu-items/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(@PathVariable Long id) {
        menuItemService.deleteMenuItem(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Menu item deleted successfully", null));
    }

    // ============ ORDERS ============

    @Operation(summary = "Create online order")
    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestParam Long restaurantId,
            @RequestParam(required = false) Long customerId,
            @RequestBody List<ItemRequest> items
    ) {
        OrderResponse response = orderService.createOrder(restaurantId, customerId, items);
        return ResponseEntity.ok(new ApiResponse<>(true, "Order created successfully", response));
    }

    @Operation(summary = "Sync offline orders - CRITICAL FOR OFFLINE-FIRST")
    @PostMapping("/orders/sync")
    public ResponseEntity<ApiResponse<SyncResult>> syncOfflineOrders(
            @RequestBody List<OfflineSyncRequest> requests
    ) {
        SyncResult result = orderService.syncOfflineOrders(requests);
        return ResponseEntity.ok(new ApiResponse<>(true, "Orders synced successfully", result));
    }

    @Operation(summary = "Get daily report for restaurant")
    @GetMapping("/orders/restaurant/{restaurantId}/daily")
    public ResponseEntity<ApiResponse<DailyReportResponse>> getDailyReport(
            @PathVariable Long restaurantId,
            @RequestParam String date
    ) {
        LocalDate localDate = LocalDate.parse(date);
        DailyReportResponse report = orderService.getDailyReport(restaurantId, localDate);
        return ResponseEntity.ok(new ApiResponse<>(true, "Daily report retrieved successfully", report));
    }

    @Operation(summary = "Get orders by status")
    @GetMapping("/orders/restaurant/{restaurantId}/status/{status}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByStatus(
            @PathVariable Long restaurantId,
            @PathVariable String status
    ) {
        // This would need a method in OrderService to implement, for now returning empty
        return ResponseEntity.ok(new ApiResponse<>(true, "Orders retrieved successfully", List.of()));
    }

    @Operation(summary = "Update order status")
    @PutMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        OrderResponse response = orderService.updateStatus(id, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Order status updated successfully", response));
    }
}
