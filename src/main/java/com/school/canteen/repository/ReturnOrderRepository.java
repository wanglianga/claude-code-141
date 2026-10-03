package com.school.canteen.repository;

import com.school.canteen.entity.ReturnOrder;
import com.school.canteen.enums.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnOrderRepository extends JpaRepository<ReturnOrder, Long> {
    boolean existsByReturnNo(String returnNo);

    List<ReturnOrder> findByStatus(ReturnStatus status);

    List<ReturnOrder> findBySupplierId(Long supplierId);
}
