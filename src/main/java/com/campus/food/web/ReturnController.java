package com.campus.food.web;

import com.campus.food.domain.ReturnOrder;
import com.campus.food.domain.enums.ReturnStatus;
import com.campus.food.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "退货")
@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @Operation(summary = "退货单列表（可按供应商/状态过滤）")
    @GetMapping
    public List<ReturnOrder> list(@RequestParam(required = false) Long supplierId,
                                  @RequestParam(required = false) ReturnStatus status) {
        return returnService.list(supplierId, status);
    }

    @Operation(summary = "退货单详情")
    @GetMapping("/{id}")
    public ReturnOrder get(@PathVariable Long id) {
        return returnService.get(id);
    }

    @Operation(summary = "确认退货完成（供应商已取回）")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('PROCUREMENT')")
    public ReturnOrder complete(@PathVariable Long id) {
        return returnService.complete(id);
    }
}
