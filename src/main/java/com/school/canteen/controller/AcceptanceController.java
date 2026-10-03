package com.school.canteen.controller;

import com.school.canteen.dto.AcceptanceDtos.CompleteRequest;
import com.school.canteen.dto.AcceptanceDtos.StartRequest;
import com.school.canteen.entity.Acceptance;
import com.school.canteen.service.AcceptanceService;
import com.school.canteen.service.ImpactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Tag(name = "验收")
@RestController
@RequestMapping("/api/acceptances")
public class AcceptanceController {

    private final AcceptanceService acceptanceService;
    private final ImpactService impactService;

    public AcceptanceController(AcceptanceService acceptanceService, ImpactService impactService) {
        this.acceptanceService = acceptanceService;
        this.impactService = impactService;
    }

    @Operation(summary = "验收单列表")
    @GetMapping
    public List<Acceptance> list() {
        return acceptanceService.list();
    }

    @Operation(summary = "验收单详情")
    @GetMapping("/{id}")
    public Acceptance get(@PathVariable Long id) {
        return acceptanceService.get(id);
    }

    @Operation(summary = "对送货批次发起验收")
    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('ADMIN','INSPECTOR')")
    public Acceptance start(@Valid @RequestBody StartRequest req, Principal principal) {
        return acceptanceService.start(req, principal.getName());
    }

    @Operation(summary = "完成验收：逐项录入重量/外观/保质期/抽检/腐烂比例等，自动入库、生成退货单/降级审批/异常")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','INSPECTOR')")
    public Acceptance complete(@PathVariable Long id, @Valid @RequestBody CompleteRequest req,
                               Principal principal) {
        return acceptanceService.complete(id, req, principal.getName());
    }

    @Operation(summary = "验收失败影响分析：受影响餐次、替代食材、家长公告、供应商责任")
    @GetMapping("/{id}/impact")
    public Map<String, Object> impact(@PathVariable Long id) {
        return impactService.impactReport(id);
    }
}
