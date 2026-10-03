package com.school.canteen.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.school.canteen.enums.ReturnStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 退货单（验收拒收后生成，等待供应商取回） */
@Getter
@Setter
@Entity
@Table(name = "return_orders")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ReturnOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String returnNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private DeliveryBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acceptance_item_id", nullable = false)
    private AcceptanceItem acceptanceItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 16)
    private String unit;

    @Column(nullable = false, length = 64)
    private String ingredientName;

    @Column(length = 512)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReturnStatus status = ReturnStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime returnedAt;
}
