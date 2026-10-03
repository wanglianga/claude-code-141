package com.campus.food.repo;

import com.campus.food.domain.Ingredient;
import com.campus.food.domain.enums.IngredientCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngredientRepo extends JpaRepository<Ingredient, Long> {
    Optional<Ingredient> findByCode(String code);
    List<Ingredient> findByCategory(IngredientCategory category);
    List<Ingredient> findBySubstituteGroup(String substituteGroup);
}
