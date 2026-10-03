package com.campus.food.service;

import com.campus.food.domain.*;
import com.campus.food.domain.enums.*;
import com.campus.food.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 复盘统计：按供应商 / 品类 / 验收员统计异常，
 * 并按归因区分天气质量波动、供应商履约不稳与验收标准执行偏差。
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final SupplierRepo supplierRepo;
    private final AppUserRepo userRepo;
    private final AnomalyRecordRepo anomalyRepo;
    private final AcceptanceRecordRepo recordRepo;
    private final AcceptanceItemRepo acceptanceItemRepo;
    private final ClaimRepo claimRepo;

    /** 按供应商统计 */
    public List<SupplierStats> bySupplier(LocalDate start, LocalDate end) {
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atTime(LocalTime.MAX);
        List<SupplierStats> result = new ArrayList<>();
        for (Supplier s : supplierRepo.findAll()) {
            List<AnomalyRecord> anomalies = anomalyRepo.findBySupplierIdAndDetectedAtBetween(s.getId(), from, to);
            Map<AnomalyType, Long> byType = anomalies.stream()
                    .collect(Collectors.groupingBy(AnomalyRecord::getType, Collectors.counting()));
            double avgMinutes = anomalies.stream()
                    .filter(a -> a.getResolvedAt() != null)
                    .mapToLong(a -> Duration.between(a.getDetectedAt(), a.getResolvedAt()).toMinutes())
                    .average().orElse(0);
            BigDecimal claimSum = claimRepo.findBySupplierId(s.getId()).stream()
                    .filter(c -> (c.getStatus() == ClaimStatus.APPROVED || c.getStatus() == ClaimStatus.PAID)
                            && !c.getCreatedAt().isBefore(from) && !c.getCreatedAt().isAfter(to))
                    .map(Claim::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            List<AcceptanceItem> items = acceptanceItemRepo.findForSupplierPeriod(s.getId(), from, to);
            BigDecimal accepted = sum(items, AcceptanceItem::getAcceptedQty);
            BigDecimal rejected = sum(items, AcceptanceItem::getRejectedQty);
            BigDecimal downgraded = sum(items, AcceptanceItem::getDowngradedQty);
            BigDecimal total = accepted.add(rejected).add(downgraded);
            double rejectRate = total.signum() == 0 ? 0
                    : rejected.add(downgraded).divide(total, 4, RoundingMode.HALF_UP).doubleValue();

            result.add(new SupplierStats(s.getId(), s.getName(), anomalies.size(), byType,
                    round2(avgMinutes), claimSum, items.size(), round4(rejectRate)));
        }
        return result;
    }

    /** 按品类统计 */
    public Map<String, Long> byCategory(LocalDate start, LocalDate end) {
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atTime(LocalTime.MAX);
        Map<String, Long> result = new LinkedHashMap<>();
        for (AnomalyRecord a : anomalyRepo.findAll()) {
            if (a.getDetectedAt().isBefore(from) || a.getDetectedAt().isAfter(to)) {
                continue;
            }
            String key = a.getCategory() == null ? "UNKNOWN" : a.getCategory().name();
            result.merge(key, 1L, Long::sum);
        }
        return result;
    }

    /** 按验收员统计 */
    public List<InspectorStats> byInspector() {
        // 验收偏差归因的异常 -> 对应批次验收人
        Map<Long, Long> deviationByInspector = new HashMap<>();
        for (AnomalyRecord a : anomalyRepo.findByCauseCategory(CauseCategory.INSPECTION_DEVIATION)) {
            if (a.getBatch() == null) {
                continue;
            }
            recordRepo.findByBatchId(a.getBatch().getId())
                    .ifPresent(r -> deviationByInspector.merge(r.getInspector().getId(), 1L, Long::sum));
        }

        List<InspectorStats> result = new ArrayList<>();
        for (AppUser u : userRepo.findAll()) {
            if (u.getRole() != Role.INSPECTOR) {
                continue;
            }
            List<AcceptanceRecord> records = recordRepo.findByInspectorId(u.getId());
            List<AcceptanceItem> items = acceptanceItemRepo.findByInspectorId(u.getId());
            BigDecimal accepted = sum(items, AcceptanceItem::getAcceptedQty);
            BigDecimal rejected = sum(items, AcceptanceItem::getRejectedQty);
            BigDecimal downgraded = sum(items, AcceptanceItem::getDowngradedQty);
            BigDecimal total = accepted.add(rejected).add(downgraded);
            double rejectRate = total.signum() == 0 ? 0
                    : rejected.add(downgraded).divide(total, 4, RoundingMode.HALF_UP).doubleValue();
            result.add(new InspectorStats(u.getId(), u.getUsername(), u.getFullName(),
                    records.size(), items.size(), round4(rejectRate),
                    deviationByInspector.getOrDefault(u.getId(), 0L)));
        }
        return result;
    }

    /** 归因分布：区分天气波动 / 供应商履约 / 验收执行偏差 */
    public CauseBreakdown causeBreakdown(LocalDate start, LocalDate end) {
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atTime(LocalTime.MAX);
        Map<String, Long> byCause = new LinkedHashMap<>();
        Map<String, Map<String, Long>> causeByType = new LinkedHashMap<>();
        long total = 0;
        for (AnomalyRecord a : anomalyRepo.findAll()) {
            if (a.getDetectedAt().isBefore(from) || a.getDetectedAt().isAfter(to)) {
                continue;
            }
            total++;
            String cause = a.getCauseCategory() == null ? "UNRESOLVED" : a.getCauseCategory().name();
            byCause.merge(cause, 1L, Long::sum);
            causeByType.computeIfAbsent(cause, k -> new LinkedHashMap<>())
                    .merge(a.getType().name(), 1L, Long::sum);
        }
        return new CauseBreakdown(total, byCause, causeByType);
    }

    private static BigDecimal sum(List<AcceptanceItem> items,
                                  java.util.function.Function<AcceptanceItem, BigDecimal> getter) {
        return items.stream().map(getter).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static double round4(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    public record SupplierStats(Long supplierId, String supplierName, long anomalyCount,
                                Map<AnomalyType, Long> anomaliesByType, double avgHandlingMinutes,
                                BigDecimal approvedClaimAmount, long acceptanceItemCount, double rejectRate) {
    }

    public record InspectorStats(Long userId, String username, String fullName, long acceptanceCount,
                                 long acceptanceItemCount, double rejectRate, long inspectionDeviationCount) {
    }

    public record CauseBreakdown(long total, Map<String, Long> byCause,
                                 Map<String, Map<String, Long>> causeByType) {
    }
}
