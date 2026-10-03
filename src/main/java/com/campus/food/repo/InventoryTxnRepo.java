package com.campus.food.repo;

import com.campus.food.domain.InventoryTxn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTxnRepo extends JpaRepository<InventoryTxn, Long> {
    List<InventoryTxn> findByIngredientIdOrderByCreatedAtDesc(Long ingredientId);
}
