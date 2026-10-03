package com.campus.food.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** 订货单明细 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private PurchaseOrder order;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    /** 规格 */
    private String spec;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    /** 合同单价 */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal contractPrice;

    /** 是否需要冷链 */
    @Column(nullable = false)
    private boolean requiresColdChain = false;

    /** 质检要求 */
    @Column(length = 512)
    private String qualityRequirements;
}
