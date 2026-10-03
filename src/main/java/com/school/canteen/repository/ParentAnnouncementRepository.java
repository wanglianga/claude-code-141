package com.school.canteen.repository;

import com.school.canteen.entity.ParentAnnouncement;
import com.school.canteen.enums.AnnouncementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParentAnnouncementRepository extends JpaRepository<ParentAnnouncement, Long> {
    List<ParentAnnouncement> findByStatus(AnnouncementStatus status);
}
