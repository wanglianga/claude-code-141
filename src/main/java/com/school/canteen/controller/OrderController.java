package com.school.canteen.controller;

import com.school.canteen.entity.PurchaseOrder;
import com.school.canteen.enums.OrderStatus;
import com.school.canteen.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "订货单")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "订货单列表")
    @GetMapping
    public List<PurchaseOrder> list(@RequestParam(required = false) OrderStatus status,
                                    @RequestParam(required = false) Long supplierId) {
        return orderService.list(status, supplierId);
    }

    @Operation(summary = "订货单详情（含规格、数量、到货时间、质检要求、合同价、冷链要求）")
    @GetMapping("/{id}")
    public PurchaseOrder get(@PathVariable Long id) {
        return orderService.get(id);
    }

    @Operation(summary = "确认下单")
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT')")
    public PurchaseOrder confirm(@PathVariable Long id) {
        return orderService.confirm(id);
    }

    @Operation(summary = "取消订货单")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT')")
    public PurchaseOrder cancel(@PathVariable Long id) {
        return orderService.cancel(id);
    }
}
