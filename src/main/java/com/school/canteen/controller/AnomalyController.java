package com.school.canteen.controller;

import com.school.canteen.dto.ActionDtos.ResolveAnomalyRequest;
import com.school.canteen.entity.Anomaly;
import com.school.canteen.enums.AnomalyStatus;
import com.school.canteen.service.AnomalyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Tag(name = "异常")
@RestController
@RequestMapping("/api/anomalies")
public class AnomalyController {

    private final AnomalyService anomalyService;

    public AnomalyController(AnomalyService anomalyService) {
        this.anomalyService = anomalyService;
    }

    @Operation(summary = "异常列表")
    @GetMapping
    public List<Anomaly> list(@RequestParam(required = false) AnomalyStatus status,
                              @RequestParam(required = false) Long supplierId) {
        return anomalyService.list(status, supplierId);
    }

    @Operation(summary = "异常详情")
    @GetMapping("/{id}")
    public Anomaly get(@PathVariable Long id) {
        return anomalyService.get(id);
    }

    @Operation(summary = "处理异常：归因（天气/供应商履约/验收标准执行偏差）并标记解决，记录处理时长")
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT')")
    public Anomaly resolve(@PathVariable Long id, @Valid @RequestBody ResolveAnomalyRequest req,
                           Principal principal) {
        return anomalyService.resolve(id, req.causeCategory(), req.resolutionNote(), principal.getName());
    }
}
