package com.school.canteen.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.school.canteen.enums.Category;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 一餐所需的食材 */
@Getter
@Setter
@Entity
@Table(name = "menu_ingredients")
public class MenuIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id", nullable = false)
    private MenuMeal meal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Category category;

    @Column(nullable = false, length = 64)
    private String ingredientName;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 16)
    private String unit;
}
