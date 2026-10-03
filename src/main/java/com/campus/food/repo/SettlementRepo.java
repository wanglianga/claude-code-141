package com.campus.food.repo;

import com.campus.food.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepo extends JpaRepository<Settlement, Long> {
    List<Settlement> findBySupplierId(Long supplierId);
}
