package com.campus.food.web;

import com.campus.food.domain.OrderItem;
import com.campus.food.domain.PurchaseOrder;
import com.campus.food.domain.enums.OrderStatus;
import com.campus.food.repo.OrderItemRepo;
import com.campus.food.repo.PurchaseOrderRepo;
import com.campus.food.service.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "订货单")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final PurchaseOrderRepo orderRepo;
    private final OrderItemRepo orderItemRepo;

    public record OrderDetail(PurchaseOrder order, List<OrderItem> items) {
    }

    @Operation(summary = "订货单列表（可按供应商/状态过滤）")
    @GetMapping
    public List<PurchaseOrder> list(@RequestParam(required = false) Long supplierId,
                                    @RequestParam(required = false) OrderStatus status) {
        if (supplierId != null) {
            return orderRepo.findBySupplierId(supplierId);
        }
        if (status != null) {
            return orderRepo.findByStatus(status);
        }
        return orderRepo.findAll();
    }

    @Operation(summary = "订货单详情（含明细）")
    @GetMapping("/{id}")
    public OrderDetail detail(@PathVariable Long id) {
        PurchaseOrder order = orderRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("订货单不存在: " + id));
        return new OrderDetail(order, orderItemRepo.findByOrderId(id));
    }
}
