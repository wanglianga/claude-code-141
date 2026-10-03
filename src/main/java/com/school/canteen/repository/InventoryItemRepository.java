package com.school.canteen.repository;

import com.school.canteen.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findByIngredientNameAndSpecAndUnit(String ingredientName, String spec, String unit);

    List<InventoryItem> findByIngredientName(String ingredientName);
}
