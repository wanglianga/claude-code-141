package com.school.canteen.entity;

import com.school.canteen.enums.MenuStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 周菜单 */
@Getter
@Setter
@Entity
@Table(name = "weekly_menus")
public class WeeklyMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String title;

    /** 周起始日（周一） */
    @Column(nullable = false, unique = true)
    private LocalDate weekStart;

    @Column(nullable = false)
    private LocalDate weekEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MenuStatus status = MenuStatus.DRAFT;

    @OneToMany(mappedBy = "menu", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("mealDate, mealType")
    private List<MenuMeal> meals = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
