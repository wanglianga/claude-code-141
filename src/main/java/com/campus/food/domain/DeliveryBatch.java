package com.campus.food.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 送货批次（供应商一次到校送货） */
@Entity
@Table(name = "delivery_batches")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String batchNo;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private PurchaseOrder order;

    /** 车牌号 */
    @Column(nullable = false, length = 32)
    private String licensePlate;

    private String driverName;

    private String driverPhone;

    /** 实际到校时间 */
    @Column(nullable = false)
    private LocalDateTime arrivedAt;

    /** 车厢实测温度（℃） */
    @Column(precision = 5, scale = 2)
    private BigDecimal vehicleTemperature;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
