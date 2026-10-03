package com.campus.food.repo;

import com.campus.food.domain.PurchaseOrder;
import com.campus.food.domain.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderRepo extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findBySupplierId(Long supplierId);
    List<PurchaseOrder> findByPlanId(Long planId);
    List<PurchaseOrder> findByStatus(OrderStatus status);
}
