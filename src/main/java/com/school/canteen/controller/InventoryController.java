package com.school.canteen.controller;

import com.school.canteen.dto.ActionDtos.ConsumeRequest;
import com.school.canteen.dto.ActionDtos.SafetyStockRequest;
import com.school.canteen.entity.InventoryItem;
import com.school.canteen.entity.InventoryTransaction;
import com.school.canteen.enums.TxnType;
import com.school.canteen.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Tag(name = "库存")
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Operation(summary = "库存列表（belowSafety 标记是否低于安全线）")
    @GetMapping
    public List<InventoryItem> list() {
        return inventoryService.list();
    }

    @Operation(summary = "低于安全库存线的食材")
    @GetMapping("/below-safety")
    public List<InventoryItem> belowSafety() {
        return inventoryService.belowSafety();
    }

    @Operation(summary = "库存流水")
    @GetMapping("/transactions")
    public List<InventoryTransaction> transactions(@RequestParam(required = false) Long itemId) {
        return inventoryService.transactions(itemId);
    }

    @Operation(summary = "调整安全库存线")
    @PutMapping("/{id}/safety-stock")
    @PreAuthorize("hasAnyRole('ADMIN','CANTEEN','PROCUREMENT')")
    public InventoryItem updateSafetyStock(@PathVariable Long id, @Valid @RequestBody SafetyStockRequest req) {
        return inventoryService.updateSafetyStock(id, req.safetyStock());
    }

    @Operation(summary = "出库（菜单领用/损耗），库存不足时失败")
    @PostMapping("/consume")
    @PreAuthorize("hasAnyRole('ADMIN','CANTEEN')")
    public List<InventoryItem> consume(@Valid @RequestBody ConsumeRequest req, Principal principal) {
        return req.lines().stream().map(line -> {
            InventoryItem item = inventoryService.get(line.inventoryItemId());
            return inventoryService.change(item.getCategory(), item.getIngredientName(), item.getSpec(),
                    item.getUnit(), line.quantity().negate(), TxnType.OUT_MENU, null, null,
                    principal.getName(), req.note() != null ? req.note() : "菜单领用出库");
        }).toList();
    }
}
