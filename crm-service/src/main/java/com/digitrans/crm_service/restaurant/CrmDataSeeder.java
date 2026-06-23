package com.digitrans.crm_service.restaurant;

import com.digitrans.crm_service.restaurant.entity.*;
import com.digitrans.crm_service.restaurant.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CrmDataSeeder implements CommandLineRunner {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    @Override
    public void run(String... args) {
        seedRestaurants();
        seedMenuItems();
        seedCustomers();
        seedOrders();
    }

    private void seedRestaurants() {
        if (restaurantRepository.count() > 0) return;

        Restaurant r1 = Restaurant.builder()
                .code("RST-DLA-001")
                .name("SavoirManger Douala Centre")
                .address("Avenue Kennedy, Douala")
                .city(City.DOUALA)
                .managerName("Jean Tomo")
                .phone("+237651234567")
                .email("douala-centre@savoirmanger.cm")
                .active(true)
                .offlineCapable(true)
                .build();

        Restaurant r2 = Restaurant.builder()
                .code("RST-DLA-002")
                .name("SavoirManger Douala Bonanjo")
                .address("Boulevard de la Liberté, Douala")
                .city(City.DOUALA)
                .managerName("Marie Boka")
                .phone("+237652345678")
                .email("douala-bonanjo@savoirmanger.cm")
                .active(true)
                .offlineCapable(true)
                .build();

        Restaurant r3 = Restaurant.builder()
                .code("RST-YDE-001")
                .name("SavoirManger Yaoundé")
                .address("Avenue Foch, Yaoundé")
                .city(City.YAOUNDE)
                .managerName("Pierre Nkom")
                .phone("+237673456789")
                .email("yaounde@savoirmanger.cm")
                .active(true)
                .offlineCapable(true)
                .build();

        restaurantRepository.saveAll(List.of(r1, r2, r3));
    }

    private void seedMenuItems() {
        if (menuItemRepository.count() > 0) return;

        List<Restaurant> restaurants = restaurantRepository.findAll();
        Restaurant r1 = restaurants.get(0);

        List<MenuItem> items = List.of(
                MenuItem.builder()
                        .code("ITEM001")
                        .name("Poulet Braisé")
                        .description("Poulet fermier braisé avec légumes")
                        .category(MenuItem_Category.PLAT_PRINCIPAL)
                        .price(new BigDecimal("3500"))
                        .available(true)
                        .restaurant(r1)
                        .build(),
                MenuItem.builder()
                        .code("ITEM002")
                        .name("Ndolé")
                        .description("Ndolé camerounais avec viande")
                        .category(MenuItem_Category.PLAT_PRINCIPAL)
                        .price(new BigDecimal("3000"))
                        .available(true)
                        .restaurant(r1)
                        .build(),
                MenuItem.builder()
                        .code("ITEM003")
                        .name("Riz Blanc")
                        .description("Riz blanc cuit à la vapeur")
                        .category(MenuItem_Category.ACCOMPAGNEMENT)
                        .price(new BigDecimal("1000"))
                        .available(true)
                        .restaurant(r1)
                        .build(),
                MenuItem.builder()
                        .code("ITEM004")
                        .name("Plantain Frit")
                        .description("Banane plantain frite")
                        .category(MenuItem_Category.ACCOMPAGNEMENT)
                        .price(new BigDecimal("1200"))
                        .available(true)
                        .restaurant(r1)
                        .build(),
                MenuItem.builder()
                        .code("ITEM005")
                        .name("Jus de Gingembre")
                        .description("Jus frais de gingembre")
                        .category(MenuItem_Category.BOISSON)
                        .price(new BigDecimal("500"))
                        .available(true)
                        .restaurant(r1)
                        .build(),
                MenuItem.builder()
                        .code("ITEM006")
                        .name("Eau Minérale")
                        .description("Eau minérale 50cl")
                        .category(MenuItem_Category.BOISSON)
                        .price(new BigDecimal("300"))
                        .available(true)
                        .restaurant(r1)
                        .build(),
                MenuItem.builder()
                        .code("ITEM007")
                        .name("Salade Verte")
                        .description("Salade verte fraîche")
                        .category(MenuItem_Category.ACCOMPAGNEMENT)
                        .price(new BigDecimal("800"))
                        .available(true)
                        .restaurant(r1)
                        .build(),
                MenuItem.builder()
                        .code("ITEM008")
                        .name("Gâteau au Chocolat")
                        .description("Gâteau au chocolat maison")
                        .category(MenuItem_Category.DESSERT)
                        .price(new BigDecimal("1500"))
                        .available(true)
                        .restaurant(r1)
                        .build(),
                MenuItem.builder()
                        .code("ITEM009")
                        .name("Fruits Tropicaux")
                        .description("Salade de fruits tropicaux")
                        .category(MenuItem_Category.DESSERT)
                        .price(new BigDecimal("2000"))
                        .available(true)
                        .restaurant(r1)
                        .build(),
                MenuItem.builder()
                        .code("ITEM010")
                        .name("Café Expresso")
                        .description("Café expresso fort")
                        .category(MenuItem_Category.BOISSON)
                        .price(new BigDecimal("400"))
                        .available(true)
                        .restaurant(r1)
                        .build()
        );

        menuItemRepository.saveAll(items);
    }

    private void seedCustomers() {
        if (customerRepository.count() > 0) return;

        Customer c1 = Customer.builder()
                .customerCode("CUST-000001")
                .firstName("Alain")
                .lastName("Kamdem")
                .phone("+237612345678")
                .email("alain@example.com")
                .city("Douala")
                .loyaltyPoints(150)
                .totalOrders(5)
                .totalSpent(new BigDecimal("25000"))
                .build();

        Customer c2 = Customer.builder()
                .customerCode("CUST-000002")
                .firstName("Céline")
                .lastName("Fonkam")
                .phone("+237687654321")
                .email("celine@example.com")
                .city("Yaoundé")
                .loyaltyPoints(80)
                .totalOrders(3)
                .totalSpent(new BigDecimal("15000"))
                .build();

        Customer c3 = Customer.builder()
                .customerCode("CUST-000003")
                .firstName("Brice")
                .lastName("Mebonde")
                .phone("+237698765432")
                .email("brice@example.com")
                .city("Douala")
                .loyaltyPoints(50)
                .totalOrders(2)
                .totalSpent(new BigDecimal("10000"))
                .build();

        customerRepository.saveAll(List.of(c1, c2, c3));
    }

    private void seedOrders() {
        if (orderRepository.count() > 0) return;

        List<Restaurant> restaurants = restaurantRepository.findAll();
        List<Customer> customers = customerRepository.findAll();
        List<MenuItem> items = menuItemRepository.findAll();

        if (restaurants.isEmpty() || customers.isEmpty() || items.isEmpty()) return;

        Restaurant r1 = restaurants.get(0);
        Customer c1 = customers.get(0);
        Customer c2 = customers.get(1);

        // Order 1
        OrderItem oi1 = OrderItem.builder()
                .menuItem(items.get(0))
                .quantity(2)
                .unitPrice(items.get(0).getPrice())
                .subtotal(items.get(0).getPrice().multiply(new BigDecimal(2)))
                .build();

        OrderItem oi2 = OrderItem.builder()
                .menuItem(items.get(2))
                .quantity(2)
                .unitPrice(items.get(2).getPrice())
                .subtotal(items.get(2).getPrice().multiply(new BigDecimal(2)))
                .build();

        Order order1 = Order.builder()
                .orderNumber("ORD-20260521-0001")
                .customer(c1)
                .restaurant(r1)
                .status(OrderStatus.DELIVERED)
                .orderMode(OrderMode.ONLINE)
                .totalAmount(new BigDecimal("8000"))
                .loyaltyPointsEarned(16)
                .orderedAt(LocalDateTime.now().minusDays(1))
                .deliveredAt(LocalDateTime.now().minusDays(1).plusHours(1))
                .items(List.of(oi1, oi2))
                .build();

        oi1.setOrder(order1);
        oi2.setOrder(order1);

        // Order 2 - offline sync
        OrderItem oi3 = OrderItem.builder()
                .menuItem(items.get(1))
                .quantity(1)
                .unitPrice(items.get(1).getPrice())
                .subtotal(items.get(1).getPrice())
                .build();

        Order order2 = Order.builder()
                .orderNumber("ORD-20260521-0002")
                .customer(c2)
                .restaurant(r1)
                .status(OrderStatus.PENDING)
                .orderMode(OrderMode.OFFLINE_SYNC)
                .offlineId("offline-uuid-001")
                .totalAmount(new BigDecimal("3000"))
                .loyaltyPointsEarned(6)
                .orderedAt(LocalDateTime.now().minusHours(3))
                .items(List.of(oi3))
                .build();

        oi3.setOrder(order2);

        orderRepository.saveAll(List.of(order1, order2));
    }
}
