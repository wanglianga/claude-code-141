package com.campus.food.repo;

import com.campus.food.domain.WeeklyMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WeeklyMenuRepo extends JpaRepository<WeeklyMenu, Long> {
    Optional<WeeklyMenu> findByWeekStart(LocalDate weekStart);
}
