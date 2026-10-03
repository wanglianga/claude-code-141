package com.school.canteen.repository;

import com.school.canteen.entity.WeeklyMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WeeklyMenuRepository extends JpaRepository<WeeklyMenu, Long> {
    Optional<WeeklyMenu> findByWeekStart(LocalDate weekStart);
}
