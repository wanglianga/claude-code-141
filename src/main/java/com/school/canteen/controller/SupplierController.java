package com.school.canteen.controller;

import com.school.canteen.entity.Supplier;
import com.school.canteen.entity.SupplierPrice;
import com.school.canteen.service.AnomalyService;
import com.school.canteen.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "供应商")
@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;
    private final AnomalyService anomalyService;

    public SupplierController(SupplierService supplierService, AnomalyService anomalyService) {
        this.supplierService = supplierService;
        this.anomalyService = anomalyService;
    }

    @Operation(summary = "供应商列表")
    @GetMapping
    public List<Supplier> list() {
        return supplierService.list();
    }

    @Operation(summary = "供应商详情")
    @GetMapping("/{id}")
    public Supplier get(@PathVariable Long id) {
        return supplierService.get(id);
    }

    @Operation(summary = "新增供应商")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT')")
    public Supplier create(@RequestBody Supplier supplier) {
        return supplierService.create(supplier);
    }

    @Operation(summary = "更新供应商")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT')")
    public Supplier update(@PathVariable Long id, @RequestBody Supplier supplier) {
        return supplierService.update(id, supplier);
    }

    @Operation(summary = "供应商合同价格表")
    @GetMapping("/{id}/prices")
    public List<SupplierPrice> prices(@PathVariable Long id) {
        return supplierService.prices(id);
    }

    @Operation(summary = "新增合同价格")
    @PostMapping("/{id}/prices")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT')")
    public SupplierPrice addPrice(@PathVariable Long id, @RequestBody SupplierPrice price) {
        return supplierService.addPrice(id, price);
    }

    @Operation(summary = "删除合同价格")
    @DeleteMapping("/prices/{priceId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT')")
    public void deletePrice(@PathVariable Long priceId) {
        supplierService.deletePrice(priceId);
    }

    @Operation(summary = "供应商异常汇总：异常类型、处理时长、对菜单执行的影响、连续异常批次")
    @GetMapping("/{id}/anomaly-summary")
    public Map<String, Object> anomalySummary(@PathVariable Long id) {
        return anomalyService.supplierSummary(id);
    }
}
