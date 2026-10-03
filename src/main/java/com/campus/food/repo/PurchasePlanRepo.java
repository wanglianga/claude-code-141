package com.campus.food.repo;

import com.campus.food.domain.PurchasePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchasePlanRepo extends JpaRepository<PurchasePlan, Long> {
    List<PurchasePlan> findByMenuId(Long menuId);
}
