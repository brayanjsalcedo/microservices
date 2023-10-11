package com.bsalcedo.inventory_service.repositories;

import com.bsalcedo.inventory_service.model.entities.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
}
