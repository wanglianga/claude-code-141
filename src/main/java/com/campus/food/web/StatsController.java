package com.campus.food.web;

import com.campus.food.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "复盘统计")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PROCUREMENT')")
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "按供应商统计（异常数/类型/处理时长/索赔/拒收率）")
    @GetMapping("/by-supplier")
    public List<StatsService.SupplierStats> bySupplier(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return statsService.bySupplier(defaultStart(start), defaultEnd(end));
    }

    @Operation(summary = "按品类统计异常")
    @GetMapping("/by-category")
    public Map<String, Long> byCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return statsService.byCategory(defaultStart(start), defaultEnd(end));
    }

    @Operation(summary = "按验收员统计（验收量/拒收率/验收执行偏差数）")
    @GetMapping("/by-inspector")
    public List<StatsService.InspectorStats> byInspector() {
        return statsService.byInspector();
    }

    @Operation(summary = "归因分布（天气波动/供应商履约/验收执行偏差）")
    @GetMapping("/cause-breakdown")
    public StatsService.CauseBreakdown causeBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return statsService.causeBreakdown(defaultStart(start), defaultEnd(end));
    }

    private static LocalDate defaultStart(LocalDate start) {
        return start == null ? LocalDate.now().minusDays(90) : start;
    }

    private static LocalDate defaultEnd(LocalDate end) {
        return end == null ? LocalDate.now() : end;
    }
}
