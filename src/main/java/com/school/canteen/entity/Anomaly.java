package com.school.canteen.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.school.canteen.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 异常记录（验收/结算过程中自动检测或人工登记） */
@Getter
@Setter
@Entity
@Table(name = "anomalies")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Anomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AnomalyType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity = Severity.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private PurchaseOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private DeliveryBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acceptance_item_id")
    private AcceptanceItem acceptanceItem;

    /** 涉及品类 */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private Category category;

    @Column(nullable = false, length = 1024)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AnomalyStatus status = AnomalyStatus.OPEN;

    /** 复盘归因 */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private CauseCategory causeCategory;

    /** 合同扣罚金额（结算时扣减） */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    /** 建议索赔金额 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal claimSuggestedAmount = BigDecimal.ZERO;

    /** 受影响餐次数（创建时按菜单计算） */
    @Column(nullable = false)
    private Integer affectedMealsCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime detectedAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;

    @Column(length = 64)
    private String resolvedBy;

    @Column(length = 512)
    private String resolutionNote;

    /** 处理时长（小时），解决时计算 */
    @Column(precision = 10, scale = 2)
    private BigDecimal processingHours;
}
