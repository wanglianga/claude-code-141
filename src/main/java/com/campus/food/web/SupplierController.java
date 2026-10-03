package com.campus.food.web;

import com.campus.food.domain.Supplier;
import com.campus.food.repo.SupplierRepo;
import com.campus.food.service.AnomalyService;
import com.campus.food.service.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "供应商管理")
@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierRepo supplierRepo;
    private final AnomalyService anomalyService;

    public record SupplierReq(@NotBlank String code, @NotBlank String name, String contactPerson,
                              String phone, String address, Boolean coldChainCapable,
                              String categories, String contractNo) {
    }

    @Operation(summary = "供应商列表")
    @GetMapping
    public List<Supplier> list() {
        return supplierRepo.findAll();
    }

    @Operation(summary = "供应商详情")
    @GetMapping("/{id}")
    public Supplier get(@PathVariable Long id) {
        return supplierRepo.findById(id).orElseThrow(() -> BusinessException.notFound("供应商不存在: " + id));
    }

    @Operation(summary = "新增供应商")
    @PostMapping
    @PreAuthorize("hasRole('PROCUREMENT')")
    public Supplier create(@Valid @RequestBody SupplierReq req) {
        supplierRepo.findByCode(req.code()).ifPresent(s -> {
            throw BusinessException.conflict("供应商编码已存在: " + req.code());
        });
        Supplier s = new Supplier();
        apply(s, req);
        return supplierRepo.save(s);
    }

    @Operation(summary = "修改供应商")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROCUREMENT')")
    public Supplier update(@PathVariable Long id, @Valid @RequestBody SupplierReq req) {
        Supplier s = get(id);
        apply(s, req);
        return supplierRepo.save(s);
    }

    @Operation(summary = "停用/启用供应商")
    @PostMapping("/{id}/active")
    @PreAuthorize("hasRole('PROCUREMENT')")
    public Supplier setActive(@PathVariable Long id, @RequestParam boolean active) {
        Supplier s = get(id);
        s.setActive(active);
        return supplierRepo.save(s);
    }

    @Operation(summary = "供应商异常看板（异常类型/处理时长/菜单影响/连续异常标记）")
    @GetMapping("/{id}/dashboard")
    public AnomalyService.SupplierDashboard dashboard(@PathVariable Long id) {
        return anomalyService.dashboard(id);
    }

    private void apply(Supplier s, SupplierReq req) {
        s.setCode(req.code());
        s.setName(req.name());
        s.setContactPerson(req.contactPerson());
        s.setPhone(req.phone());
        s.setAddress(req.address());
        s.setColdChainCapable(Boolean.TRUE.equals(req.coldChainCapable()));
        s.setCategories(req.categories());
        s.setContractNo(req.contractNo());
    }
}
