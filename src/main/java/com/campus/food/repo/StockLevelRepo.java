package com.campus.food.repo;

import com.campus.food.domain.StockLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StockLevelRepo extends JpaRepository<StockLevel, Long> {
    Optional<StockLevel> findByIngredientId(Long ingredientId);

    @Query("select s from StockLevel s where s.quantity < s.ingredient.safetyStock")
    List<StockLevel> findBelowSafetyLine();
}
