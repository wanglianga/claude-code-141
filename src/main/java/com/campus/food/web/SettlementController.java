package com.campus.food.web;

import com.campus.food.domain.Settlement;
import com.campus.food.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "结算")
@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    public record GenerateReq(@NotNull Long supplierId, @NotNull LocalDate periodStart,
                              @NotNull LocalDate periodEnd) {
    }

    public record ConfirmReq(BigDecimal supplierInvoiceAmount) {
    }

    @Operation(summary = "生成结算单（按实际验收重量、退货、折价、索赔、合同扣罚）")
    @PostMapping("/generate")
    @PreAuthorize("hasRole('PROCUREMENT')")
    public Settlement generate(@Valid @RequestBody GenerateReq req) {
        return settlementService.generate(req.supplierId(), req.periodStart(), req.periodEnd());
    }

    @Operation(summary = "结算单列表（可按供应商过滤）")
    @GetMapping
    public List<Settlement> list(@RequestParam(required = false) Long supplierId) {
        return settlementService.list(supplierId);
    }

    @Operation(summary = "结算单详情（含明细行）")
    @GetMapping("/{id}")
    public SettlementService.SettlementDetail detail(@PathVariable Long id) {
        return settlementService.detail(id);
    }

    @Operation(summary = "确认结算（录入供应商开票金额，不一致自动登记结算差异异常）")
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('PROCUREMENT')")
    public Settlement confirm(@PathVariable Long id, @RequestBody(required = false) ConfirmReq req) {
        return settlementService.confirm(id, req == null ? null : req.supplierInvoiceAmount());
    }

    @Operation(summary = "标记结算已支付")
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('PROCUREMENT')")
    public Settlement pay(@PathVariable Long id) {
        return settlementService.markPaid(id);
    }
}
