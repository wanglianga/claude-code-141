package com.campus.food.domain;

import com.campus.food.domain.enums.MenuStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 周菜单 */
@Entity
@Table(name = "weekly_menus")
@Getter
@Setter
@NoArgsConstructor
public class WeeklyMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 周一日期 */
    @Column(nullable = false, unique = true)
    private LocalDate weekStart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MenuStatus status = MenuStatus.DRAFT;

    private String remark;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
