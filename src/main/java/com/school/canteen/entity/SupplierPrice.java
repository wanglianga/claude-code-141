package com.school.canteen.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.school.canteen.enums.Category;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 供应商供货价格表（合同价来源） */
@Getter
@Setter
@Entity
@Table(name = "supplier_prices")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SupplierPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Category category;

    @Column(nullable = false, length = 64)
    private String ingredientName;

    /** 规格，如 "25kg/袋"、"500g/盒" */
    @Column(length = 64)
    private String spec;

    @Column(nullable = false, length = 16)
    private String unit;

    /** 合同单价（元/单位） */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** 是否需要冷链 */
    @Column(nullable = false)
    private boolean coldChainRequired = false;

    /** 冷链最高允许温度（℃），非冷链为空 */
    @Column(precision = 5, scale = 2)
    private BigDecimal tempMax;
}
