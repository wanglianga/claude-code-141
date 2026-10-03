package com.school.canteen.repository;

import com.school.canteen.entity.DowngradeApproval;
import com.school.canteen.enums.DowngradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DowngradeApprovalRepository extends JpaRepository<DowngradeApproval, Long> {
    List<DowngradeApproval> findByStatus(DowngradeStatus status);

    Optional<DowngradeApproval> findByAcceptanceItemId(Long acceptanceItemId);
}
