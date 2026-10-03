package com.school.canteen.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.school.canteen.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 结算单：按实际验收重量、退货、折价、索赔和合同扣罚生成 */
@Getter
@Setter
@Entity
@Table(name = "settlements")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String settlementNo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    /** 合格验收金额 = Σ 合格数量 × 合同价（含强制收货折价调整） */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal baseAmount = BigDecimal.ZERO;

    /** 降级品折价金额 = Σ 降级数量 × 合同价 × 折价率 */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal downgradeAmount = BigDecimal.ZERO;

    /** 退货扣减金额 = Σ 拒收数量 × 合同价（展示用，未计入应付） */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal returnDeduction = BigDecimal.ZERO;

    /** 索赔扣减 = Σ 已批准索赔 */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal claimDeduction = BigDecimal.ZERO;

    /** 合同扣罚 = Σ 异常扣罚 */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    /** 应付金额 = baseAmount + downgradeAmount - claimDeduction - penaltyAmount */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal finalAmount = BigDecimal.ZERO;

    /** 供应商发票金额（用于核对结算金额与验收重量是否一致） */
    @Column(precision = 14, scale = 2)
    private BigDecimal supplierInvoiceAmount;

    /** 发票金额与应付金额是否不一致 */
    @Column(nullable = false)
    private boolean mismatchFlag = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SettlementStatus status = SettlementStatus.DRAFT;

    @Column(length = 64)
    private String createdBy;

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SettlementItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime confirmedAt;

    private LocalDateTime paidAt;
}
