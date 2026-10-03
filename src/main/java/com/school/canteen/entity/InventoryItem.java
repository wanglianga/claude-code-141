package com.school.canteen.entity;

import com.school.canteen.enums.Category;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 库存（持仓） */
@Getter
@Setter
@Entity
@Table(name = "inventory_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ingredient_name", "spec", "unit"})
})
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ingredient_name", nullable = false, length = 64)
    private String ingredientName;

    @Column(length = 64)
    private String spec;

    @Column(nullable = false, length = 16)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Category category;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    /** 安全库存线 */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal safetyStock = BigDecimal.ZERO;

    @Version
    private Long version;

    @Transient
    public boolean isBelowSafety() {
        return quantity.compareTo(safetyStock) < 0;
    }
}
