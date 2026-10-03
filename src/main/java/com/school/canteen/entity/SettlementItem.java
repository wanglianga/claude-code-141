package com.school.canteen.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 结算明细 */
@Getter
@Setter
@Entity
@Table(name = "settlement_items")
public class SettlementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "acceptance_item_id", nullable = false)
    private AcceptanceItem acceptanceItem;

    @Column(nullable = false, length = 64)
    private String ingredientName;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal acceptedQty;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal downgradedQty;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal rejectedQty;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /** 该明细应付金额（合格价 + 折价价） */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
}
