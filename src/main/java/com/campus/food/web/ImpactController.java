package com.campus.food.web;

import com.campus.food.service.ImpactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "影响分析")
@RestController
@RequestMapping("/api/impact")
@RequiredArgsConstructor
public class ImpactController {

    private final ImpactService impactService;

    @Operation(summary = "按送货批次分析验收失败影响（受影响餐次/替代食材/家长公告/供应商责任）")
    @GetMapping("/batch/{batchId}")
    public ImpactService.ImpactReport byBatch(@PathVariable Long batchId) {
        return impactService.analyzeBatch(batchId);
    }

    @Operation(summary = "按验收明细分析影响")
    @GetMapping("/acceptance-item/{itemId}")
    public ImpactService.ImpactReport byAcceptanceItem(@PathVariable Long itemId) {
        return impactService.analyzeAcceptanceItem(itemId);
    }
}
