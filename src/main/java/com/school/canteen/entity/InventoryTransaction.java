package com.school.canteen.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.school.canteen.enums.TxnType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 库存流水 */
@Getter
@Setter
@Entity
@Table(name = "inventory_transactions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TxnType type;

    /** 变动数量（入正出负） */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    /** 变动后结存 */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal balanceAfter;

    /** 关联单据类型，如 ACCEPTANCE / RETURN / CPDOCTRE... */
    @Column(length = 32)
    private String refType;

    private Long refId;

    @Column(length = 64)
    private String operator;

    @Column(length = 512)
    private String note;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
