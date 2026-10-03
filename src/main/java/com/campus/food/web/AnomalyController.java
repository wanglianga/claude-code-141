package com.campus.food.web;

import com.campus.food.domain.AnomalyRecord;
import com.campus.food.domain.enums.AnomalyStatus;
import com.campus.food.domain.enums.AnomalyType;
import com.campus.food.domain.enums.CauseCategory;
import com.campus.food.service.AnomalyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "异常管理")
@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyService anomalyService;

    public record ResolveReq(@NotNull CauseCategory causeCategory, String note) {
    }

    @Operation(summary = "异常列表（可按供应商/类型/状态过滤）")
    @GetMapping
    public List<AnomalyRecord> list(@RequestParam(required = false) Long supplierId,
                                    @RequestParam(required = false) AnomalyType type,
                                    @RequestParam(required = false) AnomalyStatus status) {
        return anomalyService.list(supplierId, type, status);
    }

    @Operation(summary = "异常详情")
    @GetMapping("/{id}")
    public AnomalyRecord get(@PathVariable Long id) {
        return anomalyService.get(id);
    }

    @Operation(summary = "标记处理中")
    @PostMapping("/{id}/processing")
    @PreAuthorize("hasRole('PROCUREMENT')")
    public AnomalyRecord processing(@PathVariable Long id) {
        return anomalyService.markProcessing(id);
    }

    @Operation(summary = "关闭异常并归因（天气/供应商履约/验收执行偏差）")
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('PROCUREMENT')")
    public AnomalyRecord resolve(@PathVariable Long id, @Valid @RequestBody ResolveReq req) {
        return anomalyService.resolve(id, req.causeCategory(), req.note());
    }

    @Operation(summary = "供应商异常看板")
    @GetMapping("/dashboard/{supplierId}")
    public AnomalyService.SupplierDashboard dashboard(@PathVariable Long supplierId) {
        return anomalyService.dashboard(supplierId);
    }
}
