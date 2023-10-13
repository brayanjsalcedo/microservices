package com.bsalcedo.notification_service.events;


import com.bsalcedo.notification_service.model.enums.OrderStatus;

public record OrderEvent(String orderNumber, int itemsCount, OrderStatus orderStatus) {
}
