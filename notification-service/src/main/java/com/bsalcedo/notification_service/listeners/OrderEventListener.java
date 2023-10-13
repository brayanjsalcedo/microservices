package com.bsalcedo.notification_service.listeners;

import com.bsalcedo.notification_service.events.OrderEvent;
import com.bsalcedo.notification_service.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventListener {

    @KafkaListener(topics = "order-topic")
    public void handleOrderNotification(String message) {
        var orderEvent = JsonUtils.fromJson(message, OrderEvent.class);

        // Send email to customer, SMS, etc...
        // Notify other services
        log.info("Order {} event received for order: {} with {} items",
                orderEvent.orderStatus(),
                orderEvent.orderNumber(),
                orderEvent.itemsCount()
        );
    }
}
