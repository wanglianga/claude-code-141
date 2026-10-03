package com.school.canteen.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.school.canteen.enums.InspectResult;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 验收明细：核对车牌/温度/批次/检疫票/重量/外观/保质期/抽检结果 */
@Getter
@Setter
@Entity
@Table(name = "acceptance_items")
public class AcceptanceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acceptance_id", nullable = false)
    private Acceptance acceptance;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    /** 实际送货数量 */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal deliveredQty = BigDecimal.ZERO;

    /** 验收合格数量（入库存） */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal acceptedQty = BigDecimal.ZERO;

    /** 不合格拒收数量（退货） */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal rejectedQty = BigDecimal.ZERO;

    /** 降级使用数量（待审批） */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal downgradedQty = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean temperatureOk = true;

    @Column(nullable = false)
    private boolean appearanceOk = true;

    @Column(nullable = false)
    private boolean shelfLifeOk = true;

    @Column(nullable = false)
    private boolean samplingOk = true;

    /** 腐烂比例（%），蔬菜/水果类填写 */
    @Column(precision = 5, scale = 2)
    private BigDecimal rotRatio = BigDecimal.ZERO;

    /** 是否临时替换了品类 */
    @Column(nullable = false)
    private boolean substituted = false;

    /** 食堂已备餐无法拒收，强制收货（折价） */
    @Column(nullable = false)
    private boolean forceAccepted = false;

    /** 强制收货折价率，如 0.50 表示按 5 折计价 */
    @Column(precision = 4, scale = 2)
    private BigDecimal forcedDiscountRatio;

    @Column(length = 512)
    private String rejectReason;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private InspectResult result;
}
