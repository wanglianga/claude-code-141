package com.campus.food.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** 采购计划明细 */
@Entity
@Table(name = "purchase_plan_items")
@Getter
@Setter
@NoArgsConstructor
public class PurchasePlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id")
    private PurchasePlan plan;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    /** 菜单需求量 */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal requiredQty;

    /** 生成计划时的库存量 */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal stockQty;

    /** 计划采购量 = 需求量 - 可用库存（库存需保留安全线） */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal plannedQty;

    /** 建议供应商 */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /** 预估单价（取食材参考价） */
    @Column(precision = 14, scale = 2)
    private BigDecimal estimatedPrice;
}
