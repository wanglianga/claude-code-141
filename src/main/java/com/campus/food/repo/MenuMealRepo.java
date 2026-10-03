package com.campus.food.repo;

import com.campus.food.domain.MenuMeal;
import com.campus.food.domain.enums.MealStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface MenuMealRepo extends JpaRepository<MenuMeal, Long> {
    List<MenuMeal> findByMenuIdOrderByDateAsc(Long menuId);
    List<MenuMeal> findByDateBetweenAndStatusIn(LocalDate from, LocalDate to, Collection<MealStatus> statuses);
}
