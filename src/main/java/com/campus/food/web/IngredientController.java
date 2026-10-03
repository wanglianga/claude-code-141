package com.campus.food.web;

import com.campus.food.domain.Ingredient;
import com.campus.food.domain.enums.IngredientCategory;
import com.campus.food.repo.IngredientRepo;
import com.campus.food.service.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "食材管理")
@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientRepo ingredientRepo;

    public record IngredientReq(@NotBlank String code, @NotBlank String name,
                                @NotNull IngredientCategory category, @NotBlank String unit,
                                String spec, Boolean requiresColdChain,
                                BigDecimal coldChainMinTemp, BigDecimal coldChainMaxTemp,
                                Integer shelfLifeDays, BigDecimal safetyStock,
                                BigDecimal referencePrice, String substituteGroup) {
    }

    @Operation(summary = "食材列表（可按品类过滤）")
    @GetMapping
    public List<Ingredient> list(@RequestParam(required = false) IngredientCategory category) {
        return category == null ? ingredientRepo.findAll() : ingredientRepo.findByCategory(category);
    }

    @Operation(summary = "食材详情")
    @GetMapping("/{id}")
    public Ingredient get(@PathVariable Long id) {
        return ingredientRepo.findById(id).orElseThrow(() -> BusinessException.notFound("食材不存在: " + id));
    }

    @Operation(summary = "新增食材")
    @PostMapping
    @PreAuthorize("hasRole('PROCUREMENT')")
    public Ingredient create(@Valid @RequestBody IngredientReq req) {
        ingredientRepo.findByCode(req.code()).ifPresent(i -> {
            throw BusinessException.conflict("食材编码已存在: " + req.code());
        });
        Ingredient ing = new Ingredient();
        apply(ing, req);
        return ingredientRepo.save(ing);
    }

    @Operation(summary = "修改食材")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROCUREMENT')")
    public Ingredient update(@PathVariable Long id, @Valid @RequestBody IngredientReq req) {
        Ingredient ing = get(id);
        apply(ing, req);
        return ingredientRepo.save(ing);
    }

    private void apply(Ingredient ing, IngredientReq req) {
        ing.setCode(req.code());
        ing.setName(req.name());
        ing.setCategory(req.category());
        ing.setUnit(req.unit());
        ing.setSpec(req.spec());
        ing.setRequiresColdChain(Boolean.TRUE.equals(req.requiresColdChain()));
        ing.setColdChainMinTemp(req.coldChainMinTemp());
        ing.setColdChainMaxTemp(req.coldChainMaxTemp());
        ing.setShelfLifeDays(req.shelfLifeDays());
        if (req.safetyStock() != null) {
            ing.setSafetyStock(req.safetyStock());
        }
        if (req.referencePrice() != null) {
            ing.setReferencePrice(req.referencePrice());
        }
        ing.setSubstituteGroup(req.substituteGroup());
    }
}
