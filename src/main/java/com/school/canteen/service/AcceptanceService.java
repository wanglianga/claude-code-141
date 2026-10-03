package com.school.canteen.service;

import com.school.canteen.dto.AcceptanceDtos.CompleteRequest;
import com.school.canteen.dto.AcceptanceDtos.ItemRequest;
import com.school.canteen.dto.AcceptanceDtos.StartRequest;
import com.school.canteen.entity.*;
import com.school.canteen.enums.*;
import com.school.canteen.exception.ApiException;
import com.school.canteen.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AcceptanceService {

    /** 腐烂比例阈值（%） */
    public static final BigDecimal ROT_THRESHOLD = new BigDecimal("5");
    /** 晚送容忍分钟数 */
    public static final long LATE_TOLERANCE_MINUTES = 30;

    private final AcceptanceRepository acceptanceRepository;
    private final AcceptanceItemRepository acceptanceItemRepository;
    private final DeliveryBatchRepository batchRepository;
    private final PurchaseOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ReturnOrderRepository returnRepository;
    private final DowngradeApprovalRepository downgradeRepository;
    private final InventoryService inventoryService;
    private final AnomalyService anomalyService;
    private final NoGenerator noGenerator;

    public AcceptanceService(AcceptanceRepository acceptanceRepository, DeliveryBatchRepository batchRepository,
                             PurchaseOrderRepository orderRepository, UserRepository userRepository,
                             ReturnOrderRepository returnRepository, DowngradeApprovalRepository downgradeRepository,
                             InventoryService inventoryService, AnomalyService anomalyService, NoGenerator noGenerator,
                             AcceptanceItemRepository acceptanceItemRepository) {
        this.acceptanceRepository = acceptanceRepository;
        this.acceptanceItemRepository = acceptanceItemRepository;
        this.batchRepository = batchRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.returnRepository = returnRepository;
        this.downgradeRepository = downgradeRepository;
        this.inventoryService = inventoryService;
        this.anomalyService = anomalyService;
        this.noGenerator = noGenerator;
    }

    public List<Acceptance> list() {
        return acceptanceRepository.findAll();
    }

    public Acceptance get(Long id) {
        return acceptanceRepository.findById(id).orElseThrow(() -> ApiException.notFound("验收单不存在: " + id));
    }

    @Transactional
    public Acceptance start(StartRequest req, String inspectorUsername) {
        DeliveryBatch batch = batchRepository.findById(req.batchId())
                .orElseThrow(() -> ApiException.notFound("送货批次不存在: " + req.batchId()));
        acceptanceRepository.findByBatchId(batch.getId()).ifPresent(a -> {
            throw ApiException.conflict("该批次已发起验收: " + a.getId());
        });
        User inspector = userRepository.findByUsername(inspectorUsername)
                .orElseThrow(() -> ApiException.notFound("验收员不存在: " + inspectorUsername));
        Acceptance acceptance = new Acceptance();
        acceptance.setBatch(batch);
        acceptance.setInspector(inspector);
        acceptance.setNotes(req.notes());
        return acceptanceRepository.save(acceptance);
    }

    /**
     * 完成验收：记录各明细验收结果 → 合格入库 / 拒收退货 / 降级审批 → 自动检测异常。
     */
    @Transactional
    public Acceptance complete(Long acceptanceId, CompleteRequest req, String username) {
        Acceptance acceptance = get(acceptanceId);
        if (acceptance.getStatus() == AcceptanceStatus.COMPLETED) {
            throw ApiException.badRequest("验收单已完成，不能重复提交");
        }
        DeliveryBatch batch = acceptance.getBatch();
        PurchaseOrder order = batch.getOrder();
        Map<Long, OrderItem> orderItems = order.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));

        boolean anyRejected = false;
        boolean anyDowngraded = false;
        List<AcceptanceItem> savedItems = new ArrayList<>();

        for (ItemRequest ir : req.items()) {
            OrderItem oi = orderItems.get(ir.orderItemId());
            if (oi == null) {
                throw ApiException.badRequest("订货明细 " + ir.orderItemId() + " 不属于该批次对应的订货单");
            }
            AcceptanceItem ai = new AcceptanceItem();
            ai.setAcceptance(acceptance);
            ai.setOrderItem(oi);
            ai.setDeliveredQty(nz(ir.deliveredQty()));
            ai.setAcceptedQty(nz(ir.acceptedQty()));
            ai.setRejectedQty(nz(ir.rejectedQty()));
            ai.setDowngradedQty(nz(ir.downgradedQty()));
            ai.setTemperatureOk(ir.temperatureOk() == null || ir.temperatureOk());
            ai.setAppearanceOk(ir.appearanceOk() == null || ir.appearanceOk());
            ai.setShelfLifeOk(ir.shelfLifeOk() == null || ir.shelfLifeOk());
            ai.setSamplingOk(ir.samplingOk() == null || ir.samplingOk());
            ai.setRotRatio(ir.rotRatio() == null ? BigDecimal.ZERO : ir.rotRatio());
            ai.setSubstituted(Boolean.TRUE.equals(ir.substituted()));
            ai.setForceAccepted(Boolean.TRUE.equals(ir.forceAccepted()));
            ai.setForcedDiscountRatio(ir.forcedDiscountRatio());
            ai.setRejectReason(ir.rejectReason());

            if (ai.isForceAccepted() && ai.getRejectedQty().compareTo(BigDecimal.ZERO) > 0
                    && ai.getForcedDiscountRatio() == null) {
                throw ApiException.badRequest("强制收货必须给出折价率 forcedDiscountRatio");
            }
            // 结论
            if (ai.getRejectedQty().compareTo(BigDecimal.ZERO) > 0 && !ai.isForceAccepted()) {
                ai.setResult(ai.getAcceptedQty().compareTo(BigDecimal.ZERO) > 0
                        || ai.getDowngradedQty().compareTo(BigDecimal.ZERO) > 0
                        ? InspectResult.PARTIAL : InspectResult.REJECTED);
            } else if (ai.getDowngradedQty().compareTo(BigDecimal.ZERO) > 0 || ai.isForceAccepted()) {
                ai.setResult(InspectResult.PARTIAL);
            } else {
                ai.setResult(InspectResult.QUALIFIED);
            }
            // 显式持久化验收明细（获得 id 供退货/降级/异常关联），再加入集合
            ai = acceptanceItemRepository.save(ai);
            savedItems.add(ai);
            acceptance.getItems().add(ai);

            anyRejected = anyRejected || (ai.getRejectedQty().compareTo(BigDecimal.ZERO) > 0 && !ai.isForceAccepted());
            anyDowngraded = anyDowngraded || ai.getDowngradedQty().compareTo(BigDecimal.ZERO) > 0;
        }

        // 先保存验收明细，拿到 id 供关联
        acceptance.setOverallResult(anyRejected
                ? (acceptance.getItems().stream().allMatch(i -> i.getResult() == InspectResult.REJECTED)
                        ? InspectResult.REJECTED : InspectResult.PARTIAL)
                : (anyDowngraded || acceptance.getItems().stream().anyMatch(i -> i.getResult() == InspectResult.PARTIAL)
                        ? InspectResult.PARTIAL : InspectResult.QUALIFIED));
        acceptance.setStatus(AcceptanceStatus.COMPLETED);
        acceptance.setCompletedAt(LocalDateTime.now());
        acceptance.setNotes(req.notes() != null ? req.notes() : acceptance.getNotes());
        acceptance = acceptanceRepository.save(acceptance);

        // 入库 / 退货 / 降级 / 异常
        for (AcceptanceItem ai : savedItems) {
            OrderItem oi = ai.getOrderItem();
            BigDecimal stockIn = ai.getAcceptedQty();
            if (ai.isForceAccepted()) {
                stockIn = stockIn.add(ai.getRejectedQty()); // 已备餐无法拒收，折价收货入库
            }
            if (stockIn.compareTo(BigDecimal.ZERO) > 0) {
                inventoryService.change(oi.getCategory(), oi.getIngredientName(), oi.getSpec(), oi.getUnit(),
                        stockIn, TxnType.IN_ACCEPTANCE, "ACCEPTANCE", acceptance.getId(), username,
                        ai.isForceAccepted() && ai.getRejectedQty().compareTo(BigDecimal.ZERO) > 0
                                ? "验收入库（含强制收货折价部分）" : "验收入库");
            }
            if (ai.getRejectedQty().compareTo(BigDecimal.ZERO) > 0 && !ai.isForceAccepted()) {
                createReturn(batch, ai, oi);
            }
            if (ai.getDowngradedQty().compareTo(BigDecimal.ZERO) > 0) {
                createDowngrade(batch, ai, oi);
            }
            detectItemAnomalies(batch, order, ai, oi);
        }
        detectBatchAnomalies(batch, order, acceptance.getItems());

        // 订货单状态：所有批次都完成验收 → COMPLETED
        List<DeliveryBatch> batches = batchRepository.findByOrderId(order.getId());
        boolean allAccepted = batches.stream().allMatch(b ->
                acceptanceRepository.findByBatchId(b.getId())
                        .map(a -> a.getStatus() == AcceptanceStatus.COMPLETED).orElse(false));
        order.setStatus(allAccepted ? OrderStatus.COMPLETED : OrderStatus.DELIVERING);
        orderRepository.save(order);
        return acceptance;
    }

    private void createReturn(DeliveryBatch batch, AcceptanceItem ai, OrderItem oi) {
        ReturnOrder ro = new ReturnOrder();
        String no;
        do {
            no = noGenerator.next("RO");
        } while (returnRepository.existsByReturnNo(no));
        ro.setReturnNo(no);
        ro.setBatch(batch);
        ro.setAcceptanceItem(ai);
        ro.setSupplier(batch.getOrder().getSupplier());
        ro.setQuantity(ai.getRejectedQty());
        ro.setUnit(oi.getUnit());
        ro.setIngredientName(oi.getIngredientName());
        ro.setReason(ai.getRejectReason() != null ? ai.getRejectReason() : "验收不合格拒收");
        returnRepository.save(ro);
    }

    private void createDowngrade(DeliveryBatch batch, AcceptanceItem ai, OrderItem oi) {
        DowngradeApproval da = new DowngradeApproval();
        da.setAcceptanceItem(ai);
        da.setSupplier(batch.getOrder().getSupplier());
        da.setIngredientName(oi.getIngredientName());
        da.setQuantity(ai.getDowngradedQty());
        da.setUnit(oi.getUnit());
        da.setReason(ai.getRejectReason() != null ? ai.getRejectReason() : "质量瑕疵，申请降级使用");
        downgradeRepository.save(da);
    }

    /** 明细级异常检测 */
    private void detectItemAnomalies(DeliveryBatch batch, PurchaseOrder order, AcceptanceItem ai, OrderItem oi) {
        Supplier supplier = order.getSupplier();
        BigDecimal price = oi.getContractPrice();
        BigDecimal deliveredValue = ai.getDeliveredQty().multiply(price);

        // 少送
        if (ai.getDeliveredQty().compareTo(oi.getQuantity()) < 0) {
            BigDecimal shortage = oi.getQuantity().subtract(ai.getDeliveredQty());
            BigDecimal ratio = shortage.divide(oi.getQuantity(), 4, RoundingMode.HALF_UP);
            anomalyService.create(AnomalyType.SHORT_DELIVERY,
                    ratio.compareTo(new BigDecimal("0.2")) > 0 ? Severity.HIGH : Severity.MEDIUM,
                    supplier, order, batch, ai, oi.getCategory(),
                    "少送：订购 " + oi.getQuantity() + oi.getUnit() + "，实送 " + ai.getDeliveredQty() + oi.getUnit()
                            + "，缺 " + shortage + oi.getUnit(),
                    BigDecimal.ZERO, shortage.multiply(price));
        }
        // 冷链温度超标
        boolean tempBreach = !ai.isTemperatureOk()
                || (oi.isColdChainRequired() && batch.getTemperature() != null && oi.getTempMax() != null
                        && batch.getTemperature().compareTo(oi.getTempMax()) > 0);
        if (tempBreach) {
            anomalyService.create(AnomalyType.COLD_CHAIN_BREACH, Severity.HIGH,
                    supplier, order, batch, ai, oi.getCategory(),
                    "冷链温度超标：要求≤" + oi.getTempMax() + "℃，实测 " + batch.getTemperature() + "℃",
                    pct(deliveredValue, "0.05"),
                    ai.getRejectedQty().add(ai.getDowngradedQty()).multiply(price));
        }
        // 腐烂比例过高
        if ((oi.getCategory() == Category.VEGETABLE || oi.getCategory() == Category.FRUIT)
                && ai.getRotRatio().compareTo(ROT_THRESHOLD) > 0) {
            anomalyService.create(AnomalyType.HIGH_ROT_RATIO,
                    ai.getRotRatio().compareTo(new BigDecimal("20")) > 0 ? Severity.HIGH : Severity.MEDIUM,
                    supplier, order, batch, ai, oi.getCategory(),
                    "腐烂比例 " + ai.getRotRatio() + "% 超过阈值 " + ROT_THRESHOLD + "%",
                    BigDecimal.ZERO, ai.getRejectedQty().multiply(price));
        }
        // 临时替换品类
        if (ai.isSubstituted()) {
            anomalyService.create(AnomalyType.SUBSTITUTION, Severity.MEDIUM,
                    supplier, order, batch, ai, oi.getCategory(),
                    "临时替换品类：" + oi.getIngredientName() + " 被替换送货",
                    pct(deliveredValue, "0.05"), BigDecimal.ZERO);
        }
        // 食堂已备餐无法拒收
        if (ai.isForceAccepted() && ai.getRejectedQty().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = ai.getForcedDiscountRatio();
            anomalyService.create(AnomalyType.FORCED_ACCEPTANCE, Severity.MEDIUM,
                    supplier, order, batch, ai, oi.getCategory(),
                    "食堂已备餐无法拒收，" + ai.getRejectedQty() + oi.getUnit() + " 按 "
                            + ratio.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "% 折价收货",
                    BigDecimal.ZERO,
                    ai.getRejectedQty().multiply(price).multiply(BigDecimal.ONE.subtract(ratio)));
        }
        // 送货与验收重量不一致
        BigDecimal accounted = ai.getAcceptedQty().add(ai.getRejectedQty()).add(ai.getDowngradedQty());
        BigDecimal diff = ai.getDeliveredQty().subtract(accounted).abs();
        BigDecimal tolerance = ai.getDeliveredQty().multiply(new BigDecimal("0.005"))
                .max(new BigDecimal("0.5"));
        if (diff.compareTo(tolerance) > 0) {
            anomalyService.create(AnomalyType.WEIGHT_MISMATCH, Severity.LOW,
                    supplier, order, batch, ai, oi.getCategory(),
                    "重量不一致：送货 " + ai.getDeliveredQty() + oi.getUnit() + "，验收合计 " + accounted + oi.getUnit()
                            + "，差异 " + diff + oi.getUnit(),
                    BigDecimal.ZERO, diff.multiply(price));
        }
        // 其他质检不合格（外观/保质期/抽检）
        if (ai.getRejectedQty().compareTo(BigDecimal.ZERO) > 0
                && (!ai.isAppearanceOk() || !ai.isShelfLifeOk() || !ai.isSamplingOk())) {
            anomalyService.create(AnomalyType.QUALITY_FAILURE, Severity.MEDIUM,
                    supplier, order, batch, ai, oi.getCategory(),
                    "质检不合格拒收 " + ai.getRejectedQty() + oi.getUnit() + "：" +
                            (ai.getRejectReason() != null ? ai.getRejectReason() : "外观/保质期/抽检不合格"),
                    BigDecimal.ZERO, ai.getRejectedQty().multiply(price));
        }
    }

    /** 批次级异常检测：晚送、票证缺失 */
    private void detectBatchAnomalies(DeliveryBatch batch, PurchaseOrder order, List<AcceptanceItem> items) {
        Supplier supplier = order.getSupplier();
        // 晚送
        LocalDateTime latestExpected = order.getItems().stream()
                .map(OrderItem::getExpectedArrival).max(LocalDateTime::compareTo).orElse(null);
        if (latestExpected != null
                && batch.getArrivedAt().isAfter(latestExpected.plusMinutes(LATE_TOLERANCE_MINUTES))) {
            long lateMinutes = Duration.between(latestExpected, batch.getArrivedAt()).toMinutes();
            BigDecimal penalty = order.getTotalAmount().multiply(new BigDecimal("0.02"))
                    .max(new BigDecimal("50"));
            anomalyService.create(AnomalyType.LATE_DELIVERY,
                    lateMinutes > 120 ? Severity.HIGH : Severity.MEDIUM,
                    supplier, order, batch, null, null,
                    "晚送：要求 " + latestExpected + " 前到货，实际 " + batch.getArrivedAt() + "，晚 " + lateMinutes + " 分钟",
                    penalty, BigDecimal.ZERO);
        }
        // 票证缺失（肉类/乳制品必须提供检疫票）
        boolean certRequired = items.stream().anyMatch(ai -> {
            Category c = ai.getOrderItem().getCategory();
            return c == Category.MEAT || c == Category.DAIRY;
        });
        if (certRequired && !batch.isQuarantineCertProvided()) {
            anomalyService.create(AnomalyType.MISSING_CERTIFICATE, Severity.HIGH,
                    supplier, order, batch, null, null,
                    "检疫票缺失：肉类/乳制品送货未提供动物检疫合格证明（车牌 " + batch.getPlateNumber() + "）",
                    new BigDecimal("100"), BigDecimal.ZERO);
        }
    }

    private BigDecimal pct(BigDecimal base, String ratio) {
        return base.multiply(new BigDecimal(ratio));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
