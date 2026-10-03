package com.campus.food.web;

import com.campus.food.domain.Claim;
import com.campus.food.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "索赔")
@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    public record ClaimReq(@NotNull Long supplierId, Long batchId, Long anomalyId,
                           String claimType, @NotNull BigDecimal amount, String reason) {
    }

    public record RejectReq(String note) {
    }

    @Operation(summary = "登记索赔")
    @PostMapping
    @PreAuthorize("hasRole('PROCUREMENT')")
    public Claim create(@Valid @RequestBody ClaimReq req) {
        return claimService.create(req.supplierId(), req.batchId(), req.anomalyId(),
                req.claimType(), req.amount(), req.reason());
    }

    @Operation(summary = "索赔列表（可按供应商过滤）")
    @GetMapping
    public List<Claim> list(@RequestParam(required = false) Long supplierId) {
        return claimService.list(supplierId);
    }

    @Operation(summary = "索赔详情")
    @GetMapping("/{id}")
    public Claim get(@PathVariable Long id) {
        return claimService.get(id);
    }

    @Operation(summary = "批准索赔")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('PROCUREMENT')")
    public Claim approve(@PathVariable Long id) {
        return claimService.approve(id);
    }

    @Operation(summary = "驳回索赔")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('PROCUREMENT')")
    public Claim reject(@PathVariable Long id, @RequestBody(required = false) RejectReq req) {
        return claimService.reject(id, req == null ? null : req.note());
    }

    @Operation(summary = "标记索赔已支付")
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('PROCUREMENT')")
    public Claim pay(@PathVariable Long id) {
        return claimService.markPaid(id);
    }
}
