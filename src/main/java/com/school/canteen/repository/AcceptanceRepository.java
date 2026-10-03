package com.school.canteen.repository;

import com.school.canteen.entity.Acceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcceptanceRepository extends JpaRepository<Acceptance, Long> {
    Optional<Acceptance> findByBatchId(Long batchId);
}
