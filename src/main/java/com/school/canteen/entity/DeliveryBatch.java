package com.school.canteen.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 供应商送货批次 */
@Getter
@Setter
@Entity
@Table(name = "delivery_batches")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DeliveryBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String batchNo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder order;

    /** 车牌号 */
    @Column(nullable = false, length = 16)
    private String plateNumber;

    /** 实际到校时间 */
    @Column(nullable = false)
    private LocalDateTime arrivedAt;

    /** 车厢实测温度（℃），非冷链可为空 */
    @Column(precision = 5, scale = 2)
    private BigDecimal temperature;

    /** 供应商批次号 */
    @Column(length = 64)
    private String supplierBatchNo;

    /** 是否提供检疫票 */
    @Column(nullable = false)
    private boolean quarantineCertProvided = false;

    /** 检疫票编号 */
    @Column(length = 64)
    private String quarantineCertNo;

    @Column(length = 64)
    private String receivedBy;

    @Column(length = 512)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
