package com.school.canteen.controller;

import com.school.canteen.dto.ActionDtos.SettlementGenerateRequest;
import com.school.canteen.entity.Settlement;
import com.school.canteen.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Tag(name = "结算")
@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @Operation(summary = "结算单列表")
    @GetMapping
    public List<Settlement> list(@RequestParam(required = false) Long supplierId) {
        return settlementService.list(supplierId);
    }

    @Operation(summary = "结算单详情（含明细：实际验收重量、退货、折价、索赔、扣罚）")
    @GetMapping("/{id}")
    public Settlement get(@PathVariable Long id) {
        return settlementService.get(id);
    }

    @Operation(summary = "生成结算单：按实际验收重量计价，扣减退货/折价/索赔/合同扣罚；可传供应商发票金额自动核对")
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    public Settlement generate(@Valid @RequestBody SettlementGenerateRequest req, Principal principal) {
        return settlementService.generate(req.supplierId(), req.periodStart(), req.periodEnd(),
                req.supplierInvoiceAmount(), principal.getName());
    }

    @Operation(summary = "确认结算单")
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    public Settlement confirm(@PathVariable Long id) {
        return settlementService.confirm(id);
    }

    @Operation(summary = "标记已付款")
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    public Settlement pay(@PathVariable Long id) {
        return settlementService.pay(id);
    }
}
