package com.bsalcedo.orders_service.services;

import com.bsalcedo.orders_service.events.OrderEvent;
import com.bsalcedo.orders_service.model.dtos.*;
import com.bsalcedo.orders_service.model.entities.Order;
import com.bsalcedo.orders_service.model.entities.OrderItems;
import com.bsalcedo.orders_service.model.enums.OrderStatus;
import com.bsalcedo.orders_service.repositories.OrderRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.bsalcedo.orders_service.utils.JsonUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObservationRegistry observationRegistry;

    public OrderResponse placeOrder(OrderRequest orderRequest) {

        Observation inventoryObservation = Observation.createNotStarted("inventory-service", observationRegistry);

        return inventoryObservation.observe(() -> {
            // Check for inventory
            BaseResponse result = this.webClientBuilder.build()
                    .post()
                    .uri("lb://inventory-service/api/inventory/in-stock")
                    .bodyValue(orderRequest.getOrderItems())
                    .retrieve()
                    .bodyToMono(BaseResponse.class)
                    .block();

            if (result != null && !result.hasErrors()) {
                Order order = new Order();
                order.setOrderNumber(UUID.randomUUID().toString());
                order.setOrderItems(orderRequest.getOrderItems().stream()
                        .map(orderItemRequest -> mapOrderItemRequestToOrderItem(orderItemRequest, order))
                        .toList());

                var savedOrder = this.orderRepository.save(order);

                // Send message to order topic
                this.kafkaTemplate.send("order-topic", JsonUtils.toJson(
                        new OrderEvent(savedOrder.getOrderNumber(), savedOrder.getOrderItems().size(), OrderStatus.PLACED)
                ));

                return mapToOrderResponse(savedOrder);
            } else {
                throw new IllegalArgumentException("Some of the products are not available in stock");
            }
        });
    }

    public List<OrderResponse> getAllOrders() {
        List<Order> orders = this.orderRepository.findAll();

        return orders.stream().map(this::mapToOrderResponse).toList();
    }

    private OrderResponse mapToOrderResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderItems().stream().map(this::mapToOrderItemsResponse).toList()
        );
    }

    private OrderItemsResponse mapToOrderItemsResponse(OrderItems orderItems) {
        return new OrderItemsResponse(orderItems.getId(), orderItems.getSku(), orderItems.getPrice(), orderItems.getQuantity());
    }

    private OrderItems mapOrderItemRequestToOrderItem(OrderItemRequest orderItemRequest, Order order) {
        return OrderItems.builder()
                .id(orderItemRequest.getId())
                .sku(orderItemRequest.getSku())
                .price(orderItemRequest.getPrice())
                .quantity(orderItemRequest.getQuantity())
                .order(order)
                .build();
    }
}