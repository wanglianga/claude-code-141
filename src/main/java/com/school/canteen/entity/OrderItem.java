package com.school.canteen.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.school.canteen.enums.Category;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订货单明细 */
@Getter
@Setter
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Category category;

    @Column(nullable = false, length = 64)
    private String ingredientName;

    /** 规格 */
    @Column(length = 64)
    private String spec;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 16)
    private String unit;

    /** 要求到货时间 */
    @Column(nullable = false)
    private LocalDateTime expectedArrival;

    /** 质检要求描述 */
    @Column(length = 512)
    private String qualityRequirement;

    /** 合同单价 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal contractPrice;

    /** 是否需要冷链 */
    @Column(nullable = false)
    private boolean coldChainRequired = false;

    /** 冷链最高允许温度（℃） */
    @Column(precision = 5, scale = 2)
    private BigDecimal tempMax;
}
