package com.school.canteen.repository;

import com.school.canteen.entity.DeliveryBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryBatchRepository extends JpaRepository<DeliveryBatch, Long> {
    boolean existsByBatchNo(String batchNo);

    List<DeliveryBatch> findByOrderId(Long orderId);

    List<DeliveryBatch> findByOrderSupplierIdOrderByArrivedAtDesc(Long supplierId);
}
