package com.campus.food.domain;

import com.campus.food.domain.enums.SettlementItemType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** 结算明细行 */
@Entity
@Table(name = "settlement_items")
@Getter
@Setter
@NoArgsConstructor
public class SettlementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "settlement_id")
    private Settlement settlement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SettlementItemType itemType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    /** 关联验收明细（扣罚/索赔行可为空） */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "acceptance_item_id")
    private AcceptanceItem acceptanceItem;

    @Column(precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(precision = 14, scale = 2)
    private BigDecimal unitPrice;

    /** 行金额（扣减行为负数） */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(length = 256)
    private String note;
}
