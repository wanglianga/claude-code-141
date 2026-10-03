package com.school.canteen.repository;

import com.school.canteen.entity.AcceptanceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AcceptanceItemRepository extends JpaRepository<AcceptanceItem, Long> {

    List<AcceptanceItem> findByAcceptanceId(Long acceptanceId);

    /** 某供应商在结算周期内已验收完成、且尚未纳入任何结算单的验收明细 */
    @Query("select ai from AcceptanceItem ai join ai.acceptance a join a.batch b join b.order o " +
            "where o.supplier.id = :supplierId " +
            "and a.status = com.school.canteen.enums.AcceptanceStatus.COMPLETED " +
            "and a.completedAt between :from and :to " +
            "and not exists (select 1 from SettlementItem si where si.acceptanceItem = ai)")
    List<AcceptanceItem> findUnsettledBySupplierAndPeriod(@Param("supplierId") Long supplierId,
                                                          @Param("from") LocalDateTime from,
                                                          @Param("to") LocalDateTime to);
}
