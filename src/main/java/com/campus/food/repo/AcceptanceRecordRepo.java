package com.campus.food.repo;

import com.campus.food.domain.AcceptanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcceptanceRecordRepo extends JpaRepository<AcceptanceRecord, Long> {
    boolean existsByBatchId(Long batchId);
    Optional<AcceptanceRecord> findByBatchId(Long batchId);
    List<AcceptanceRecord> findByInspectorId(Long inspectorId);
}
