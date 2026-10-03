package com.school.canteen.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.school.canteen.enums.DowngradeStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 降级使用审批 */
@Getter
@Setter
@Entity
@Table(name = "downgrade_approvals")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DowngradeApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acceptance_item_id", nullable = false)
    private AcceptanceItem acceptanceItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(nullable = false, length = 64)
    private String ingredientName;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 16)
    private String unit;

    /** 降级原因，如 "外观轻微损伤，可加工后使用" */
    @Column(length = 512)
    private String reason;

    /** 折价率，如 0.60 表示按合同价 6 折计价 */
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal priceRatio = new BigDecimal("0.60");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DowngradeStatus status = DowngradeStatus.PENDING;

    @Column(length = 64)
    private String approver;

    private LocalDateTime decidedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
