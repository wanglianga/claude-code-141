package com.school.canteen.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.school.canteen.enums.AnnouncementStatus;
import com.school.canteen.enums.MealType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 家长公告（食材验收异常影响餐次时发布） */
@Getter
@Setter
@Entity
@Table(name = "parent_announcements")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParentAnnouncement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 256)
    private String title;

    @Column(nullable = false, length = 2048)
    private String content;

    /** 受影响餐次日期 */
    private LocalDate mealDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private MealType mealType;

    @Column(length = 256)
    private String dishName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anomaly_id")
    private Anomaly anomaly;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AnnouncementStatus status = AnnouncementStatus.DRAFT;

    @Column(length = 64)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime publishedAt;
}
