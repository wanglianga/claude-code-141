package com.campus.food.repo;

import com.campus.food.domain.SettlementItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementItemRepo extends JpaRepository<SettlementItem, Long> {
    List<SettlementItem> findBySettlementId(Long settlementId);
}
