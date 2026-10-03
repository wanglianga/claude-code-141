package com.school.canteen.repository;

import com.school.canteen.entity.Anomaly;
import com.school.canteen.enums.AnomalyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AnomalyRepository extends JpaRepository<Anomaly, Long> {
    List<Anomaly> findBySupplierId(Long supplierId);

    List<Anomaly> findByStatus(AnomalyStatus status);

    List<Anomaly> findByDetectedAtBetween(LocalDateTime from, LocalDateTime to);

    List<Anomaly> findBySupplierIdAndDetectedAtBetween(Long supplierId, LocalDateTime from, LocalDateTime to);

    List<Anomaly> findByBatchId(Long batchId);
}
