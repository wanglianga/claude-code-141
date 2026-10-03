package com.school.canteen.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.school.canteen.enums.AcceptanceStatus;
import com.school.canteen.enums.InspectResult;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 验收单（一个送货批次对应一张验收单） */
@Getter
@Setter
@Entity
@Table(name = "acceptances")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Acceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id", nullable = false, unique = true)
    private DeliveryBatch batch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "inspector_id", nullable = false)
    private User inspector;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AcceptanceStatus status = AcceptanceStatus.IN_PROGRESS;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private InspectResult overallResult;

    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    @Column(length = 512)
    private String notes;

    @OneToMany(mappedBy = "acceptance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AcceptanceItem> items = new ArrayList<>();
}
