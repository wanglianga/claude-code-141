package com.school.canteen.repository;

import com.school.canteen.entity.SupplierPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierPriceRepository extends JpaRepository<SupplierPrice, Long> {
    List<SupplierPrice> findBySupplierId(Long supplierId);

    List<SupplierPrice> findByIngredientNameAndUnit(String ingredientName, String unit);

    List<SupplierPrice> findByIngredientName(String ingredientName);
}
