package com.school.canteen.service;

import com.school.canteen.entity.*;
import com.school.canteen.enums.*;
import com.school.canteen.exception.ApiException;
import com.school.canteen.repository.AnomalyRepository;
import com.school.canteen.repository.DeliveryBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnomalyService {

    private final AnomalyRepository anomalyRepository;
    private final DeliveryBatchRepository batchRepository;
    private final ImpactService impactService;

    public AnomalyService(AnomalyRepository anomalyRepository, DeliveryBatchRepository batchRepository,
                          ImpactService impactService) {
        this.anomalyRepository = anomalyRepository;
        this.batchRepository = batchRepository;
        this.impactService = impactService;
    }

    public List<Anomaly> list(AnomalyStatus status, Long supplierId) {
        if (status != null) {
            return anomalyRepository.findByStatus(status);
        }
        if (supplierId != null) {
            return anomalyRepository.findBySupplierId(supplierId);
        }
        return anomalyRepository.findAll();
    }

    public Anomaly get(Long id) {
        return anomalyRepository.findById(id).orElseThrow(() -> ApiException.notFound("异常不存在: " + id));
    }

    /** 创建异常（业务事务内调用），自动计算受影响餐次数 */
    @Transactional(propagation = Propagation.REQUIRED)
    public Anomaly create(AnomalyType type, Severity severity, Supplier supplier, PurchaseOrder order,
                          DeliveryBatch batch, AcceptanceItem acceptanceItem, Category category,
                          String description, BigDecimal penalty, BigDecimal claimSuggested) {
        Anomaly a = new Anomaly();
        a.setType(type);
        a.setSeverity(severity);
        a.setSupplier(supplier);
        a.setOrder(order);
        a.setBatch(batch);
        a.setAcceptanceItem(acceptanceItem);
        a.setCategory(category);
        a.setDescription(description);
        a.setPenaltyAmount(penalty == null ? BigDecimal.ZERO : penalty.setScale(2, RoundingMode.HALF_UP));
        a.setClaimSuggestedAmount(claimSuggested == null ? BigDecimal.ZERO
                : claimSuggested.setScale(2, RoundingMode.HALF_UP));
        if (acceptanceItem != null && acceptanceItem.getOrderItem() != null) {
            a.setAffectedMealsCount(impactService.countUpcomingMeals(
                    acceptanceItem.getOrderItem().getIngredientName()));
        }
        return anomalyRepository.save(a);
    }

    /** 处理异常：归因（天气/供应商履约/验收标准执行偏差）并标记解决 */
    @Transactional
    public Anomaly resolve(Long id, CauseCategory cause, String note, String username) {
        Anomaly a = get(id);
        if (a.getStatus() == AnomalyStatus.RESOLVED || a.getStatus() == AnomalyStatus.CLOSED) {
            throw ApiException.badRequest("异常已处理完毕: " + id);
        }
        a.setCauseCategory(cause);
        a.setResolutionNote(note);
        a.setResolvedBy(username);
        a.setResolvedAt(LocalDateTime.now());
        a.setStatus(AnomalyStatus.RESOLVED);
        long minutes = Duration.between(a.getDetectedAt(), a.getResolvedAt()).toMinutes();
        a.setProcessingHours(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
        return anomalyRepository.save(a);
    }

    /**
     * 供应商异常汇总：异常类型分布、平均处理时长、对菜单执行的影响、连续异常批次数。
     */
    public Map<String, Object> supplierSummary(Long supplierId) {
        List<Anomaly> anomalies = anomalyRepository.findBySupplierId(supplierId);

        Map<String, Long> byType = anomalies.stream()
                .collect(Collectors.groupingBy(a -> a.getType().name(), LinkedHashMap::new, Collectors.counting()));

        List<Anomaly> resolved = anomalies.stream().filter(a -> a.getProcessingHours() != null).toList();
        BigDecimal avgHours = resolved.isEmpty() ? BigDecimal.ZERO
                : resolved.stream().map(Anomaly::getProcessingHours)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(resolved.size()), 2, RoundingMode.HALF_UP);

        int affectedMeals = anomalies.stream().mapToInt(Anomaly::getAffectedMealsCount).sum();

        // 连续异常：从最近一批送货往回数，每批只要有未关闭异常即连续异常批次
        List<DeliveryBatch> batches = batchRepository.findByOrderSupplierIdOrderByArrivedAtDesc(supplierId);
        Set<Long> anomalousBatchIds = anomalies.stream()
                .filter(a -> a.getBatch() != null && a.getStatus() != AnomalyStatus.CLOSED)
                .map(a -> a.getBatch().getId())
                .collect(Collectors.toSet());
        int consecutive = 0;
        for (DeliveryBatch b : batches) {
            if (anomalousBatchIds.contains(b.getId())) {
                consecutive++;
            } else {
                break;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("supplierId", supplierId);
        result.put("totalAnomalies", anomalies.size());
        result.put("openAnomalies", anomalies.stream().filter(a -> a.getStatus() == AnomalyStatus.OPEN).count());
        result.put("byType", byType);
        result.put("avgProcessingHours", avgHours);
        result.put("affectedMealsTotal", affectedMeals);
        result.put("consecutiveAnomalyBatches", consecutive);
        result.put("continuousAnomaly", consecutive >= 2);
        return result;
    }
}
