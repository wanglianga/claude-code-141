package com.school.canteen.repository;

import com.school.canteen.entity.PurchaseOrder;
import com.school.canteen.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    boolean existsByOrderNo(String orderNo);

    List<PurchaseOrder> findByStatus(OrderStatus status);

    List<PurchaseOrder> findBySupplierId(Long supplierId);

    List<PurchaseOrder> findByMenuId(Long menuId);
}
