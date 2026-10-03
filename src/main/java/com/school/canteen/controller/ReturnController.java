package com.school.canteen.controller;

import com.school.canteen.entity.ReturnOrder;
import com.school.canteen.enums.ReturnStatus;
import com.school.canteen.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "退货")
@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    private final ReturnService returnService;

    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @Operation(summary = "退货单列表")
    @GetMapping
    public List<ReturnOrder> list(@RequestParam(required = false) ReturnStatus status,
                                  @RequestParam(required = false) Long supplierId) {
        return returnService.list(status, supplierId);
    }

    @Operation(summary = "确认供应商已取回退货")
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','INSPECTOR','CANTEEN')")
    public ReturnOrder confirm(@PathVariable Long id) {
        return returnService.confirmReturned(id);
    }
}
