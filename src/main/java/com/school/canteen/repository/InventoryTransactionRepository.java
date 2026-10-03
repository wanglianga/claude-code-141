package com.school.canteen.repository;

import com.school.canteen.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByItemIdOrderByCreatedAtDesc(Long itemId);
}
