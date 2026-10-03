package com.campus.food.domain;

import com.campus.food.domain.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 结算单（按供应商+周期生成） */
@Entity
@Table(name = "settlements")
@Getter
@Setter
@NoArgsConstructor
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String billNo;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SettlementStatus status = SettlementStatus.DRAFT;

    /** 验收应付总额（按实际验收重量×合同价） */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal deliveredAmount = BigDecimal.ZERO;

    /** 退货扣减 */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal returnDeduction = BigDecimal.ZERO;

    /** 降级折价扣减 */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal downgradeDeduction = BigDecimal.ZERO;

    /** 索赔扣减 */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal claimDeduction = BigDecimal.ZERO;

    /** 合同扣罚 */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal penaltyDeduction = BigDecimal.ZERO;

    /** 最终应付 = delivered - return - downgrade - claim - penalty */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal finalAmount = BigDecimal.ZERO;

    /** 供应商开票金额（确认时录入，用于对账） */
    @Column(precision = 14, scale = 2)
    private BigDecimal supplierInvoiceAmount;

    /** 开票金额与系统结算是否不一致 */
    @Column(nullable = false)
    private boolean mismatchFlag = false;

    @Column(nullable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    private LocalDateTime confirmedAt;
}
