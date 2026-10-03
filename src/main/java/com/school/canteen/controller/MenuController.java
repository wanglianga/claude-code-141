package com.school.canteen.controller;

import com.school.canteen.entity.WeeklyMenu;
import com.school.canteen.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "周菜单与采购计划")
@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @Operation(summary = "菜单列表")
    @GetMapping
    public List<WeeklyMenu> list() {
        return menuService.list();
    }

    @Operation(summary = "菜单详情（含餐次与食材需求）")
    @GetMapping("/{id}")
    public WeeklyMenu get(@PathVariable Long id) {
        return menuService.get(id);
    }

    @Operation(summary = "创建周菜单（含餐次、食材）")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT','CANTEEN')")
    public WeeklyMenu create(@RequestBody WeeklyMenu menu) {
        return menuService.create(menu);
    }

    @Operation(summary = "发布菜单")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT','CANTEEN')")
    public WeeklyMenu publish(@PathVariable Long id) {
        return menuService.publish(id);
    }

    @Operation(summary = "按周菜单生成采购计划（汇总需求→匹配最低价供应商→按供应商生成订货单）")
    @PostMapping("/{id}/generate-plan")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT')")
    public Map<String, Object> generatePlan(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expectedArrival,
            Principal principal) {
        return menuService.generatePlan(id, expectedArrival, principal.getName());
    }
}
