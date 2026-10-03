package com.campus.food.service;

import com.campus.food.domain.*;
import com.campus.food.domain.enums.ReturnStatus;
import com.campus.food.repo.ReturnOrderRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 退货服务 */
@Service
@RequiredArgsConstructor
public class ReturnService {

    private final ReturnOrderRepo returnRepo;

    /** 验收拒收时创建退货单 */
    @Transactional
    public ReturnOrder createInternal(Supplier supplier, AcceptanceItem acceptanceItem, Ingredient ingredient,
                                      BigDecimal quantity, String reason, BigDecimal refundAmount) {
        ReturnOrder r = new ReturnOrder();
        r.setReturnNo(NoGen.next("RT"));
        r.setSupplier(supplier);
        r.setAcceptanceItem(acceptanceItem);
        r.setIngredient(ingredient);
        r.setQuantity(quantity);
        r.setReason(reason);
        r.setRefundAmount(refundAmount);
        return returnRepo.save(r);
    }

    public ReturnOrder get(Long id) {
        return returnRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("退货单不存在: " + id));
    }

    public List<ReturnOrder> list(Long supplierId, ReturnStatus status) {
        if (supplierId != null) {
            return returnRepo.findBySupplierId(supplierId);
        }
        return status != null ? returnRepo.findByStatus(status) : returnRepo.findAll();
    }

    /** 供应商已取回退货 */
    @Transactional
    public ReturnOrder complete(Long id) {
        ReturnOrder r = returnRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("退货单不存在: " + id));
        if (r.getStatus() != ReturnStatus.PENDING) {
            throw BusinessException.conflict("退货单已处理");
        }
        r.setStatus(ReturnStatus.COMPLETED);
        r.setProcessedAt(LocalDateTime.now());
        return returnRepo.save(r);
    }
}
