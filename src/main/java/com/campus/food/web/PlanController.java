package com.campus.food.web;

import com.campus.food.domain.PurchaseOrder;
import com.campus.food.domain.PurchasePlan;
import com.campus.food.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "采购计划")
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    public record GeneratePlanReq(String note) {
    }

    public record CreateOrdersReq(@NotNull LocalDateTime expectedArrivalAt, String remark) {
    }

    @Operation(summary = "计划列表")
    @GetMapping
    public List<PurchasePlan> list() {
        return planService.listPlans();
    }

    @Operation(summary = "计划详情（含明细）")
    @GetMapping("/{id}")
    public PlanService.PlanDetail detail(@PathVariable Long id) {
        return planService.detail(id);
    }

    @Operation(summary = "由周菜单生成采购计划（汇总需求，扣除可用库存并保留安全线）")
    @PostMapping("/menus/{menuId}/generate")
    @PreAuthorize("hasRole('CANTEEN')")
    public PurchasePlan generate(@PathVariable Long menuId, @RequestBody(required = false) GeneratePlanReq req) {
        return planService.generatePlan(menuId, req == null ? null : req.note());
    }

    @Operation(summary = "按计划生成订货单（按供应商拆单）")
    @PostMapping("/{id}/orders")
    @PreAuthorize("hasRole('PROCUREMENT')")
    public List<PurchaseOrder> createOrders(@PathVariable Long id, @Valid @RequestBody CreateOrdersReq req) {
        return planService.createOrders(id, req.expectedArrivalAt(), req.remark());
    }
}
