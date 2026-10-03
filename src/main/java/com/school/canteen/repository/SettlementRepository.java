package com.school.canteen.repository;

import com.school.canteen.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    boolean existsBySettlementNo(String settlementNo);

    List<Settlement> findBySupplierId(Long supplierId);
}
