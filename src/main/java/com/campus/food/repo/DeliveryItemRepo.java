package com.campus.food.repo;

import com.campus.food.domain.DeliveryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DeliveryItemRepo extends JpaRepository<DeliveryItem, Long> {
    List<DeliveryItem> findByBatchId(Long batchId);

    /** 同一订货明细在其他批次中的累计送货量（用于少送判定） */
    @Query("select coalesce(sum(di.deliveredQty), 0) from DeliveryItem di " +
           "where di.orderItem.id = :orderItemId and di.batch.id <> :excludeBatchId")
    BigDecimal sumDeliveredExcludingBatch(@Param("orderItemId") Long orderItemId,
                                          @Param("excludeBatchId") Long excludeBatchId);
}
