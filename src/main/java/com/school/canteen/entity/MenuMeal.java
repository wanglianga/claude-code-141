package com.school.canteen.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.school.canteen.enums.MealType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 菜单中的一餐 */
@Getter
@Setter
@Entity
@Table(name = "menu_meals")
public class MenuMeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private WeeklyMenu menu;

    @Column(nullable = false)
    private LocalDate mealDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MealType mealType;

    /** 菜品名称，如 "番茄炒蛋+米饭" */
    @Column(nullable = false, length = 256)
    private String dishName;

    /** 供餐份数 */
    @Column(nullable = false)
    private Integer servings = 0;

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MenuIngredient> ingredients = new ArrayList<>();
}
