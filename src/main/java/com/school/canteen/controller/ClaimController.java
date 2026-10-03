package com.school.canteen.controller;

import com.school.canteen.dto.ActionDtos.ClaimDecisionRequest;
import com.school.canteen.dto.ActionDtos.ClaimRequest;
import com.school.canteen.entity.Claim;
import com.school.canteen.enums.ClaimStatus;
import com.school.canteen.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Tag(name = "索赔")
@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @Operation(summary = "索赔单列表")
    @GetMapping
    public List<Claim> list(@RequestParam(required = false) ClaimStatus status,
                            @RequestParam(required = false) Long supplierId) {
        return claimService.list(status, supplierId);
    }

    @Operation(summary = "基于异常发起索赔")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT')")
    public Claim create(@Valid @RequestBody ClaimRequest req, Principal principal) {
        return claimService.create(req.anomalyId(), req.amount(), req.reason(), principal.getName());
    }

    @Operation(summary = "索赔审批")
    @PostMapping("/{id}/decide")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    public Claim decide(@PathVariable Long id, @Valid @RequestBody ClaimDecisionRequest req, Principal principal) {
        return claimService.decide(id, req.approve(), req.note(), principal.getName());
    }

    @Operation(summary = "标记索赔已赔付")
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    public Claim pay(@PathVariable Long id) {
        return claimService.pay(id);
    }
}
