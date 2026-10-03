package com.campus.food.domain;

import com.campus.food.domain.enums.InventoryTxnType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 库存流水 */
@Entity
@Table(name = "inventory_txns")
@Getter
@Setter
@NoArgsConstructor
public class InventoryTxn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    /** 变动数量（正为入，负为出） */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal changeQty;

    /** 变动后结存 */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InventoryTxnType type;

    /** 来源单据类型：ACCEPTANCE / RETURN / MANUAL / DOWNGRADE */
    @Column(length = 32)
    private String refType;

    private Long refId;

    @Column(length = 256)
    private String remark;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
