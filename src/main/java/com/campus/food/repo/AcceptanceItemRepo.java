package com.campus.food.repo;

import com.campus.food.domain.AcceptanceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AcceptanceItemRepo extends JpaRepository<AcceptanceItem, Long> {

    List<AcceptanceItem> findByRecordId(Long recordId);

    @Query("select ai from AcceptanceItem ai " +
           "where ai.record.batch.order.supplier.id = :supplierId " +
           "and ai.record.inspectedAt between :from and :to")
    List<AcceptanceItem> findForSupplierPeriod(@Param("supplierId") Long supplierId,
                                               @Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);

    @Query("select ai from AcceptanceItem ai where ai.record.inspector.id = :inspectorId")
    List<AcceptanceItem> findByInspectorId(@Param("inspectorId") Long inspectorId);
}
