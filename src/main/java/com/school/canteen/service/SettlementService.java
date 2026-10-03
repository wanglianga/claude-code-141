package com.school.canteen.service;

import com.school.canteen.entity.*;
import com.school.canteen.enums.*;
import com.school.canteen.exception.ApiException;
import com.school.canteen.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 结算服务：按实际验收重量、退货、折价、索赔和合同扣罚生成账单；
 * 若供应商发票金额与按验收重量计算的应付金额不一致，自动登记 SETTLEMENT_MISMATCH 异常。
 */
@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final AcceptanceItemRepository acceptanceItemRepository;
    private final ClaimRepository claimRepository;
    private final AnomalyRepository anomalyRepository;
    private final DowngradeApprovalRepository downgradeRepository;
    private final SupplierRepository supplierRepository;
    private final AnomalyService anomalyService;
    private final NoGenerator noGenerator;

    public SettlementService(SettlementRepository settlementRepository,
                             AcceptanceItemRepository acceptanceItemRepository,
                             ClaimRepository claimRepository, AnomalyRepository anomalyRepository,
                             DowngradeApprovalRepository downgradeRepository,
                             SupplierRepository supplierRepository, AnomalyService anomalyService,
                             NoGenerator noGenerator) {
        this.settlementRepository = settlementRepository;
        this.acceptanceItemRepository = acceptanceItemRepository;
        this.claimRepository = claimRepository;
        this.anomalyRepository = anomalyRepository;
        this.downgradeRepository = downgradeRepository;
        this.supplierRepository = supplierRepository;
        this.anomalyService = anomalyService;
        this.noGenerator = noGenerator;
    }

    public List<Settlement> list(Long supplierId) {
        return supplierId != null ? settlementRepository.findBySupplierId(supplierId) : settlementRepository.findAll();
    }

    public Settlement get(Long id) {
        return settlementRepository.findById(id).orElseThrow(() -> ApiException.notFound("结算单不存在: " + id));
    }

    @Transactional
    public Settlement generate(Long supplierId, LocalDate periodStart, LocalDate periodEnd,
                               BigDecimal invoiceAmount, String username) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> ApiException.notFound("供应商不存在: " + supplierId));
        LocalDateTime from = periodStart.atStartOfDay();
        LocalDateTime to = periodEnd.atTime(LocalTime.MAX);

        List<AcceptanceItem> items = acceptanceItemRepository.findUnsettledBySupplierAndPeriod(supplierId, from, to);
        if (items.isEmpty()) {
            throw ApiException.badRequest("该供应商在此周期内没有待结算的验收明细");
        }

        Settlement st = new Settlement();
        String no;
        do {
            no = noGenerator.next("ST");
        } while (settlementRepository.existsBySettlementNo(no));
        st.setSettlementNo(no);
        st.setSupplier(supplier);
        st.setPeriodStart(periodStart);
        st.setPeriodEnd(periodEnd);
        st.setCreatedBy(username);
        st.setSupplierInvoiceAmount(invoiceAmount);

        BigDecimal base = BigDecimal.ZERO;
        BigDecimal downgradeTotal = BigDecimal.ZERO;
        BigDecimal returnDeduction = BigDecimal.ZERO;

        for (AcceptanceItem ai : items) {
            OrderItem oi = ai.getOrderItem();
            BigDecimal price = oi.getContractPrice();

            BigDecimal acceptedValue = ai.getAcceptedQty().multiply(price);
            BigDecimal forcedValue = BigDecimal.ZERO;
            if (ai.isForceAccepted() && ai.getRejectedQty().compareTo(BigDecimal.ZERO) > 0) {
                // 已备餐无法拒收：按折价率计价
                forcedValue = ai.getRejectedQty().multiply(price).multiply(ai.getForcedDiscountRatio());
            } else if (ai.getRejectedQty().compareTo(BigDecimal.ZERO) > 0) {
                returnDeduction = returnDeduction.add(ai.getRejectedQty().multiply(price));
            }

            BigDecimal downgradeValue = BigDecimal.ZERO;
            if (ai.getDowngradedQty().compareTo(BigDecimal.ZERO) > 0) {
                var approval = downgradeRepository.findByAcceptanceItemId(ai.getId());
                if (approval.isPresent() && approval.get().getStatus() == DowngradeStatus.APPROVED) {
                    downgradeValue = ai.getDowngradedQty().multiply(price).multiply(approval.get().getPriceRatio());
                }
                // 未审批/未通过的降级数量暂不结算
            }

            base = base.add(acceptedValue).add(forcedValue);
            downgradeTotal = downgradeTotal.add(downgradeValue);

            SettlementItem si = new SettlementItem();
            si.setSettlement(st);
            si.setAcceptanceItem(ai);
            si.setIngredientName(oi.getIngredientName());
            si.setAcceptedQty(ai.getAcceptedQty());
            si.setDowngradedQty(ai.getDowngradedQty());
            si.setRejectedQty(ai.getRejectedQty());
            si.setUnitPrice(price);
            si.setAmount(acceptedValue.add(forcedValue).add(downgradeValue)
                    .setScale(2, RoundingMode.HALF_UP));
            st.getItems().add(si);
        }

        // 已批准索赔（周期内发起）
        BigDecimal claimDeduction = claimRepository
                .findBySupplierIdAndStatusAndCreatedAtBetween(supplierId, ClaimStatus.APPROVED, from, to)
                .stream().map(Claim::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 合同扣罚（周期内检测到的异常）
        List<Anomaly> anomalies = anomalyRepository.findBySupplierIdAndDetectedAtBetween(supplierId, from, to);
        BigDecimal penalty = anomalies.stream().map(Anomaly::getPenaltyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalAmount = base.add(downgradeTotal).subtract(claimDeduction).subtract(penalty);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        st.setBaseAmount(base.setScale(2, RoundingMode.HALF_UP));
        st.setDowngradeAmount(downgradeTotal.setScale(2, RoundingMode.HALF_UP));
        st.setReturnDeduction(returnDeduction.setScale(2, RoundingMode.HALF_UP));
        st.setClaimDeduction(claimDeduction.setScale(2, RoundingMode.HALF_UP));
        st.setPenaltyAmount(penalty.setScale(2, RoundingMode.HALF_UP));
        st.setFinalAmount(finalAmount.setScale(2, RoundingMode.HALF_UP));

        // 结算金额与验收重量核对
        if (invoiceAmount != null && invoiceAmount.subtract(st.getFinalAmount()).abs()
                .compareTo(new BigDecimal("0.01")) > 0) {
            st.setMismatchFlag(true);
        }
        st = settlementRepository.save(st);

        if (st.isMismatchFlag()) {
            anomalyService.create(AnomalyType.SETTLEMENT_MISMATCH, Severity.MEDIUM,
                    supplier, null, null, null, null,
                    "结算金额与验收重量不一致：结算单 " + st.getSettlementNo() + " 按验收重量应付 "
                            + st.getFinalAmount() + " 元，供应商发票 " + invoiceAmount + " 元，差额 "
                            + invoiceAmount.subtract(st.getFinalAmount()).abs() + " 元",
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }
        return st;
    }

    @Transactional
    public Settlement confirm(Long id) {
        Settlement st = get(id);
        if (st.getStatus() != SettlementStatus.DRAFT) {
            throw ApiException.badRequest("仅草稿状态的结算单可以确认");
        }
        st.setStatus(SettlementStatus.CONFIRMED);
        st.setConfirmedAt(LocalDateTime.now());
        return settlementRepository.save(st);
    }

    @Transactional
    public Settlement pay(Long id) {
        Settlement st = get(id);
        if (st.getStatus() != SettlementStatus.CONFIRMED) {
            throw ApiException.badRequest("仅已确认的结算单可以付款");
        }
        st.setStatus(SettlementStatus.PAID);
        st.setPaidAt(LocalDateTime.now());
        return settlementRepository.save(st);
    }
}
