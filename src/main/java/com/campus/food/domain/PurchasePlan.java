package com.campus.food.domain;

import com.campus.food.domain.enums.PlanStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 采购计划（由周菜单生成） */
@Entity
@Table(name = "purchase_plans")
@Getter
@Setter
@NoArgsConstructor
public class PurchasePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String planNo;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "menu_id")
    private WeeklyMenu menu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PlanStatus status = PlanStatus.DRAFT;

    @Column(nullable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    private String note;
}
