package com.digitrans.crm_service.restaurant.service;

import com.digitrans.crm_service.restaurant.dto.*;
import com.digitrans.crm_service.restaurant.entity.*;
import com.digitrans.crm_service.restaurant.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;

    @Transactional
    public OrderResponse createOrder(Long restaurantId, Long customerId, List<ItemRequest> items) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found: " + restaurantId));

        Customer customer = null;
        if (customerId != null) {
            customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));
        }

        String orderNumber = generateOrderNumber(restaurantId);

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ItemRequest itemReq : items) {
            MenuItem menuItem = menuItemRepository.findById(itemReq.menuItemId())
                    .orElseThrow(() -> new RuntimeException("Menu item not found: " + itemReq.menuItemId()));

            BigDecimal subtotal = menuItem.getPrice().multiply(new BigDecimal(itemReq.quantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .menuItem(menuItem)
                    .quantity(itemReq.quantity())
                    .unitPrice(menuItem.getPrice())
                    .subtotal(subtotal)
                    .build();
            orderItems.add(orderItem);
        }

        Integer loyaltyPointsEarned = customer != null ? totalAmount.divide(new BigDecimal(500)).intValue() : 0;

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customer(customer)
                .restaurant(restaurant)
                .status(OrderStatus.PENDING)
                .orderMode(OrderMode.ONLINE)
                .totalAmount(totalAmount)
                .loyaltyPointsEarned(loyaltyPointsEarned)
                .orderedAt(LocalDateTime.now())
                .items(orderItems)
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        Order saved = orderRepository.save(order);

        if (customer != null) {
            customer.setTotalOrders(customer.getTotalOrders() + 1);
            customer.setTotalSpent(customer.getTotalSpent().add(totalAmount));
            customer.setLastVisit(LocalDateTime.now());
            customerService.addLoyaltyPoints(customer.getId(), totalAmount);
            customerRepository.save(customer);
        }

        return toOrderResponse(saved);
    }

    @Transactional
    public SyncResult syncOfflineOrders(List<OfflineSyncRequest> syncRequests) {
        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (OfflineSyncRequest req : syncRequests) {
            try {
                var existing = orderRepository.findByOfflineId(req.offlineId());
                if (existing.isPresent()) {
                    skipped++;
                    continue;
                }

                Restaurant restaurant = restaurantRepository.findById(req.restaurantId())
                        .orElseThrow(() -> new RuntimeException("Restaurant not found: " + req.restaurantId()));

                Customer customer = null;
                if (req.customerId() != null) {
                    customer = customerRepository.findById(req.customerId()).orElse(null);
                }

                String orderNumber = generateOrderNumber(req.restaurantId());

                List<OrderItem> orderItems = new ArrayList<>();
                BigDecimal totalAmount = req.totalAmount();

                for (ItemRequest itemReq : req.items()) {
                    MenuItem menuItem = menuItemRepository.findById(itemReq.menuItemId())
                            .orElseThrow(() -> new RuntimeException("Menu item not found: " + itemReq.menuItemId()));

                    OrderItem orderItem = OrderItem.builder()
                            .menuItem(menuItem)
                            .quantity(itemReq.quantity())
                            .unitPrice(menuItem.getPrice())
                            .subtotal(menuItem.getPrice().multiply(new BigDecimal(itemReq.quantity())))
                            .build();
                    orderItems.add(orderItem);
                }

                Order order = Order.builder()
                        .orderNumber(orderNumber)
                        .customer(customer)
                        .restaurant(restaurant)
                        .status(OrderStatus.PENDING)
                        .orderMode(OrderMode.OFFLINE_SYNC)
                        .offlineId(req.offlineId())
                        .totalAmount(totalAmount)
                        .orderedAt(req.orderedAt())
                        .items(orderItems)
                        .build();

                orderItems.forEach(item -> item.setOrder(order));
                orderRepository.save(order);

                if (customer != null) {
                    customer.setTotalOrders(customer.getTotalOrders() + 1);
                    customer.setTotalSpent(customer.getTotalSpent().add(totalAmount));
                    customer.setLastVisit(LocalDateTime.now());
                    customerService.addLoyaltyPoints(customer.getId(), totalAmount);
                    customerRepository.save(customer);
                }

                created++;
            } catch (Exception e) {
                errors.add("Error syncing offlineId " + req.offlineId() + ": " + e.getMessage());
            }
        }

        return new SyncResult(created, skipped, errors);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        OrderStatus status = OrderStatus.valueOf(newStatus);
        order.setStatus(status);

        if (status == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        Order saved = orderRepository.save(order);
        return toOrderResponse(saved);
    }

    public DailyReportResponse getDailyReport(Long restaurantId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<Order> orders = orderRepository.findByRestaurantIdAndOrderedAtBetween(restaurantId, start, end);
        int totalOrders = orders.size();

        BigDecimal totalRevenue = orderRepository.sumTotalAmountByRestaurantAndDateRange(restaurantId, start, end);
        BigDecimal avgTicket = totalOrders > 0 ? totalRevenue.divide(new BigDecimal(totalOrders), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return new DailyReportResponse(totalOrders, totalRevenue, avgTicket);
    }

    private String generateOrderNumber(Long restaurantId) {
        LocalDate today = LocalDate.now();
        long sequence = orderRepository.count() % 10000 + 1;
        return String.format("ORD-%d%02d%02d-%04d", today.getYear(), today.getMonthValue(), today.getDayOfMonth(), sequence);
    }

    private OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomer() != null ? order.getCustomer().getId() : null,
                order.getCustomer() != null ? order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName() : "Anonymous",
                order.getRestaurant().getId(),
                order.getStatus(),
                order.getOrderMode(),
                order.getOfflineId(),
                order.getTotalAmount(),
                order.getLoyaltyPointsEarned(),
                order.getOrderedAt(),
                order.getDeliveredAt(),
                order.getItems().stream()
                        .map(item -> new OrderItemResponse(
                                item.getId(),
                                item.getMenuItem().getId(),
                                item.getMenuItem().getName(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getSubtotal()
                        ))
                        .collect(Collectors.toList())
        );
    }
}
