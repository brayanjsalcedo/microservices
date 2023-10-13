package com.bsalcedo.orders_service.events;

import com.bsalcedo.orders_service.model.enums.OrderStatus;

public record OrderEvent(String orderNumber, int itemsCount, OrderStatus orderStatus) {
}
