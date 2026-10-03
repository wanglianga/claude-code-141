package com.school.canteen.service;

import com.school.canteen.entity.InventoryItem;
import com.school.canteen.entity.InventoryTransaction;
import com.school.canteen.enums.Category;
import com.school.canteen.enums.TxnType;
import com.school.canteen.exception.ApiException;
import com.school.canteen.repository.InventoryItemRepository;
import com.school.canteen.repository.InventoryTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InventoryService {

    private final InventoryItemRepository itemRepository;
    private final InventoryTransactionRepository txnRepository;

    public InventoryService(InventoryItemRepository itemRepository, InventoryTransactionRepository txnRepository) {
        this.itemRepository = itemRepository;
        this.txnRepository = txnRepository;
    }

    public List<InventoryItem> list() {
        return itemRepository.findAll();
    }

    public List<InventoryItem> belowSafety() {
        return itemRepository.findAll().stream().filter(InventoryItem::isBelowSafety).toList();
    }

    public List<InventoryTransaction> transactions(Long itemId) {
        return itemId != null ? txnRepository.findByItemIdOrderByCreatedAtDesc(itemId) : txnRepository.findAll();
    }

    public InventoryItem get(Long id) {
        return itemRepository.findById(id).orElseThrow(() -> ApiException.notFound("库存项不存在: " + id));
    }

    /** 入库/出库记账（正数入库，负数出库），在同一事务内被业务服务调用 */
    @Transactional(propagation = Propagation.REQUIRED)
    public InventoryItem change(Category category, String ingredientName, String spec, String unit,
                                BigDecimal delta, TxnType type, String refType, Long refId,
                                String operator, String note) {
        InventoryItem item = itemRepository.findByIngredientNameAndSpecAndUnit(ingredientName, spec, unit)
                .orElseGet(() -> {
                    InventoryItem it = new InventoryItem();
                    it.setIngredientName(ingredientName);
                    it.setSpec(spec);
                    it.setUnit(unit);
                    it.setCategory(category);
                    it.setQuantity(BigDecimal.ZERO);
                    it.setSafetyStock(BigDecimal.ZERO);
                    return it;
                });
        BigDecimal after = item.getQuantity().add(delta);
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw ApiException.badRequest("库存不足: " + ingredientName + " 当前 " + item.getQuantity() + unit
                    + "，需要出库 " + delta.negate() + unit);
        }
        item.setQuantity(after);
        item = itemRepository.save(item);

        InventoryTransaction txn = new InventoryTransaction();
        txn.setItem(item);
        txn.setType(type);
        txn.setQuantity(delta);
        txn.setBalanceAfter(after);
        txn.setRefType(refType);
        txn.setRefId(refId);
        txn.setOperator(operator);
        txn.setNote(note);
        txnRepository.save(txn);
        return item;
    }

    @Transactional
    public InventoryItem updateSafetyStock(Long id, BigDecimal safetyStock) {
        InventoryItem item = get(id);
        item.setSafetyStock(safetyStock);
        return itemRepository.save(item);
    }
}
