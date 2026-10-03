package com.school.canteen.controller;

import com.school.canteen.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "复盘统计")
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @Operation(summary = "异常统计：按供应商/品类/验收员分组，含类型分布、归因、处理时长、扣罚、影响餐次")
    @GetMapping("/anomalies")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT','FINANCE')")
    public Map<String, Object> anomalies(
            @RequestParam(defaultValue = "supplier") String groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statsService.anomalyStats(groupBy, from, to);
    }

    @Operation(summary = "归因统计：区分天气导致的质量波动、供应商履约不稳、验收标准执行偏差")
    @GetMapping("/anomalies/causes")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT','FINANCE')")
    public Map<String, Object> causes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statsService.causeStats(from, to);
    }
}
