package com.campus.food.repo;

import com.campus.food.domain.Claim;
import com.campus.food.domain.enums.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ClaimRepo extends JpaRepository<Claim, Long> {
    List<Claim> findBySupplierId(Long supplierId);
    List<Claim> findByBatchId(Long batchId);
    List<Claim> findBySupplierIdAndStatusInAndCreatedAtBetween(Long supplierId,
                                                               Collection<ClaimStatus> statuses,
                                                               LocalDateTime from,
                                                               LocalDateTime to);
}
