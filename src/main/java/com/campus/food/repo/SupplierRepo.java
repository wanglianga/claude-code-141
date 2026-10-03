package com.campus.food.repo;

import com.campus.food.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierRepo extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByCode(String code);
}
