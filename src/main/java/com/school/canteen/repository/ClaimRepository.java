package com.school.canteen.repository;

import com.school.canteen.entity.Claim;
import com.school.canteen.enums.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    boolean existsByClaimNo(String claimNo);

    List<Claim> findBySupplierId(Long supplierId);

    List<Claim> findByStatus(ClaimStatus status);

    List<Claim> findBySupplierIdAndStatusAndCreatedAtBetween(Long supplierId, ClaimStatus status,
                                                             LocalDateTime from, LocalDateTime to);
}
