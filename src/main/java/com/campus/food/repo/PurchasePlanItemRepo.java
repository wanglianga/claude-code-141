package com.campus.food.repo;

import com.campus.food.domain.PurchasePlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchasePlanItemRepo extends JpaRepository<PurchasePlanItem, Long> {
    List<PurchasePlanItem> findByPlanId(Long planId);
}
