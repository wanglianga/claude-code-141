package com.school.canteen.controller;

import com.school.canteen.dto.ActionDtos.DowngradeDecisionRequest;
import com.school.canteen.entity.DowngradeApproval;
import com.school.canteen.enums.DowngradeStatus;
import com.school.canteen.service.DowngradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Tag(name = "降级使用审批")
@RestController
@RequestMapping("/api/downgrades")
public class DowngradeController {

    private final DowngradeService downgradeService;

    public DowngradeController(DowngradeService downgradeService) {
        this.downgradeService = downgradeService;
    }

    @Operation(summary = "降级审批列表")
    @GetMapping
    public List<DowngradeApproval> list(@RequestParam(required = false) DowngradeStatus status) {
        return downgradeService.list(status);
    }

    @Operation(summary = "审批降级使用（批准后按折价入库）")
    @PostMapping("/{id}/decide")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT','CANTEEN')")
    public DowngradeApproval decide(@PathVariable Long id, @Valid @RequestBody DowngradeDecisionRequest req,
                                    Principal principal) {
        return downgradeService.decide(id, req.approve(), req.priceRatio(), principal.getName());
    }
}
