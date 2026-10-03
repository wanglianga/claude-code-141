package com.school.canteen.service;

import com.school.canteen.entity.PurchaseOrder;
import com.school.canteen.enums.OrderStatus;
import com.school.canteen.exception.ApiException;
import com.school.canteen.repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final PurchaseOrderRepository orderRepository;

    public OrderService(PurchaseOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<PurchaseOrder> list(OrderStatus status, Long supplierId) {
        if (status != null) {
            return orderRepository.findByStatus(status);
        }
        if (supplierId != null) {
            return orderRepository.findBySupplierId(supplierId);
        }
        return orderRepository.findAll();
    }

    public PurchaseOrder get(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> ApiException.notFound("订货单不存在: " + id));
    }

    @Transactional
    public PurchaseOrder confirm(Long id) {
        PurchaseOrder order = get(id);
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw ApiException.badRequest("仅草稿状态的订货单可以确认下单");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        return orderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder cancel(Long id) {
        PurchaseOrder order = get(id);
        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.CONFIRMED) {
            throw ApiException.badRequest("当前状态不允许取消: " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}
