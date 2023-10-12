package com.bsalcedo.inventory_service.services;

import com.bsalcedo.inventory_service.model.dtos.BaseResponse;
import com.bsalcedo.inventory_service.model.dtos.OrderItemRequest;
import com.bsalcedo.inventory_service.model.entities.Inventory;
import com.bsalcedo.inventory_service.repositories.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    public boolean isInStock(String sku) {
        return this.inventoryRepository.findBySku(sku)
                .map(inventory -> inventory.getQuantity() > 0)
                .orElse(false);
    }

    public BaseResponse areInStock(List<OrderItemRequest> orderItemRequests) {
        var errorList = new ArrayList<String>();

        List<String> skus = orderItemRequests.stream()
                .map(OrderItemRequest::getSku)
                .toList();
        List<Inventory> inventoryList = inventoryRepository.findBySkuIn(skus);

        orderItemRequests.forEach(orderItemRequest -> {
            inventoryList.stream()
                    .filter(inventory -> inventory.getSku().equals(orderItemRequest.getSku()))
                    .findFirst()
                    .ifPresentOrElse(inventory -> {
                        if (inventory.getQuantity() < orderItemRequest.getQuantity()) {
                            errorList.add(String.format("Not enough stock for product with sku %s", inventory.getSku()));
                        }
                    }, () -> errorList.add(String.format("Product with sku %s not found", orderItemRequest.getSku())));
        });

        return !errorList.isEmpty()
                ? new BaseResponse(errorList.toArray(new String[0]))
                : new BaseResponse(null);
    }

}
