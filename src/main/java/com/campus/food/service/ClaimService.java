package com.campus.food.service;

import com.campus.food.domain.*;
import com.campus.food.domain.enums.ClaimStatus;
import com.campus.food.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 索赔服务 */
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepo claimRepo;
    private final SupplierRepo supplierRepo;
    private final DeliveryBatchRepo batchRepo;
    private final AnomalyRecordRepo anomalyRepo;

    /** 系统内自动建索赔（如已备餐无法拒收） */
    @Transactional
    public Claim createInternal(Supplier supplier, DeliveryBatch batch, AnomalyRecord anomaly,
                                String claimType, BigDecimal amount, String reason) {
        Claim c = new Claim();
        c.setClaimNo(NoGen.next("CL"));
        c.setSupplier(supplier);
        c.setBatch(batch);
        c.setAnomaly(anomaly);
        c.setClaimType(claimType);
        c.setAmount(amount);
        c.setReason(reason);
        return claimRepo.save(c);
    }

    /** 采购负责人手工登记索赔 */
    @Transactional
    public Claim create(Long supplierId, Long batchId, Long anomalyId,
                        String claimType, BigDecimal amount, String reason) {
        Supplier supplier = supplierRepo.findById(supplierId)
                .orElseThrow(() -> BusinessException.notFound("供应商不存在: " + supplierId));
        DeliveryBatch batch = batchId == null ? null : batchRepo.findById(batchId)
                .orElseThrow(() -> BusinessException.notFound("送货批次不存在: " + batchId));
        AnomalyRecord anomaly = anomalyId == null ? null : anomalyRepo.findById(anomalyId)
                .orElseThrow(() -> BusinessException.notFound("异常不存在: " + anomalyId));
        if (amount == null || amount.signum() <= 0) {
            throw BusinessException.badRequest("索赔金额必须大于 0");
        }
        return createInternal(supplier, batch, anomaly, claimType, amount, reason);
    }

    public List<Claim> list(Long supplierId) {
        return supplierId != null ? claimRepo.findBySupplierId(supplierId) : claimRepo.findAll();
    }

    public Claim get(Long id) {
        return claimRepo.findById(id).orElseThrow(() -> BusinessException.notFound("索赔单不存在: " + id));
    }

    @Transactional
    public Claim approve(Long id) {
        Claim c = get(id);
        if (c.getStatus() != ClaimStatus.PENDING) {
            throw BusinessException.conflict("仅待处理索赔可审批");
        }
        c.setStatus(ClaimStatus.APPROVED);
        c.setResolvedAt(LocalDateTime.now());
        return claimRepo.save(c);
    }

    @Transactional
    public Claim reject(Long id, String note) {
        Claim c = get(id);
        if (c.getStatus() != ClaimStatus.PENDING) {
            throw BusinessException.conflict("仅待处理索赔可驳回");
        }
        c.setStatus(ClaimStatus.REJECTED);
        c.setResolvedAt(LocalDateTime.now());
        c.setReason(c.getReason() + " | 驳回: " + note);
        return claimRepo.save(c);
    }

    @Transactional
    public Claim markPaid(Long id) {
        Claim c = get(id);
        if (c.getStatus() != ClaimStatus.APPROVED) {
            throw BusinessException.conflict("仅已批准索赔可标记支付");
        }
        c.setStatus(ClaimStatus.PAID);
        return claimRepo.save(c);
    }
}
