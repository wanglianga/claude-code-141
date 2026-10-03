package com.school.canteen.entity;

import com.school.canteen.enums.Category;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "suppliers")
public class Supplier {

    public enum Status { ACTIVE, SUSPENDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String name;

    @Column(length = 64)
    private String contact;

    @Column(length = 32)
    private String phone;

    @Column(length = 256)
    private String address;

    /** 可供货品类 */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "supplier_categories", joinColumns = @JoinColumn(name = "supplier_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 32)
    private Set<Category> categories = new HashSet<>();

    /** 是否具备冷链配送能力 */
    @Column(nullable = false)
    private boolean coldChainCapable = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
