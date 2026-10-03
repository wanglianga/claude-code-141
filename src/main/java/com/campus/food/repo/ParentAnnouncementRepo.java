package com.campus.food.repo;

import com.campus.food.domain.ParentAnnouncement;
import com.campus.food.domain.enums.AnnouncementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParentAnnouncementRepo extends JpaRepository<ParentAnnouncement, Long> {
    List<ParentAnnouncement> findByStatus(AnnouncementStatus status);
    List<ParentAnnouncement> findByMealId(Long mealId);
}
