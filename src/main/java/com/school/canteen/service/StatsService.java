package com.school.canteen.service;

import com.school.canteen.entity.Anomaly;
import com.school.canteen.entity.User;
import com.school.canteen.enums.CauseCategory;
import com.school.canteen.repository.AnomalyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 校方复盘统计：按供应商 / 品类 / 验收员统计异常，
 * 并按归因区分天气导致的质量波动、供应商履约不稳、验收标准执行偏差。
 */
@Service
public class StatsService {

    private final AnomalyRepository anomalyRepository;

    public StatsService(AnomalyRepository anomalyRepository) {
        this.anomalyRepository = anomalyRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> anomalyStats(String groupBy, LocalDate from, LocalDate to) {
        LocalDateTime f = (from != null ? from : LocalDate.now().minusMonths(3)).atStartOfDay();
        LocalDateTime t = (to != null ? to : LocalDate.now()).atTime(LocalTime.MAX);
        List<Anomaly> anomalies = anomalyRepository.findByDetectedAtBetween(f, t);

        Map<String, List<Anomaly>> groups = switch (groupBy == null ? "supplier" : groupBy) {
            case "category" -> anomalies.stream().collect(Collectors.groupingBy(
                    a -> a.getCategory() != null ? a.getCategory().name() : "未分类", LinkedHashMap::new, Collectors.toList()));
            case "inspector" -> anomalies.stream().collect(Collectors.groupingBy(this::inspectorName,
                    LinkedHashMap::new, Collectors.toList()));
            default -> anomalies.stream().collect(Collectors.groupingBy(
                    a -> a.getSupplier().getName(), LinkedHashMap::new, Collectors.toList()));
        };

        List<Map<String, Object>> rows = groups.entrySet().stream().map(e -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("group", e.getKey());
            row.put("total", e.getValue().size());
            row.put("byType", e.getValue().stream().collect(Collectors.groupingBy(
                    a -> a.getType().name(), Collectors.counting())));
            row.put("byCause", causeBreakdown(e.getValue()));
            row.put("avgProcessingHours", avgHours(e.getValue()));
            row.put("penaltyTotal", e.getValue().stream().map(Anomaly::getPenaltyAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            row.put("affectedMealsTotal", e.getValue().stream().mapToInt(Anomaly::getAffectedMealsCount).sum());
            return row;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupBy", groupBy == null ? "supplier" : groupBy);
        result.put("from", f.toLocalDate());
        result.put("to", t.toLocalDate());
        result.put("totalAnomalies", anomalies.size());
        result.put("rows", rows);
        return result;
    }

    /** 归因分布：区分天气 / 供应商履约 / 验收标准执行偏差 */
    @Transactional(readOnly = true)
    public Map<String, Object> causeStats(LocalDate from, LocalDate to) {
        LocalDateTime f = (from != null ? from : LocalDate.now().minusMonths(3)).atStartOfDay();
        LocalDateTime t = (to != null ? to : LocalDate.now()).atTime(LocalTime.MAX);
        List<Anomaly> anomalies = anomalyRepository.findByDetectedAtBetween(f, t);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", f.toLocalDate());
        result.put("to", t.toLocalDate());
        result.put("total", anomalies.size());
        result.put("weather", countByCause(anomalies, CauseCategory.WEATHER));
        result.put("supplierPerformance", countByCause(anomalies, CauseCategory.SUPPLIER_PERFORMANCE));
        result.put("inspectionDeviation", countByCause(anomalies, CauseCategory.INSPECTION_DEVIATION));
        result.put("other", countByCause(anomalies, CauseCategory.OTHER));
        result.put("unclassified", anomalies.stream().filter(a -> a.getCauseCategory() == null).count());
        return result;
    }

    private Map<String, Long> causeBreakdown(List<Anomaly> list) {
        return list.stream().collect(Collectors.groupingBy(
                a -> a.getCauseCategory() != null ? a.getCauseCategory().name() : "UNCLASSIFIED",
                Collectors.counting()));
    }

    private long countByCause(List<Anomaly> list, CauseCategory cause) {
        return list.stream().filter(a -> a.getCauseCategory() == cause).count();
    }

    private BigDecimal avgHours(List<Anomaly> list) {
        List<Anomaly> resolved = list.stream().filter(a -> a.getProcessingHours() != null).toList();
        if (resolved.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return resolved.stream().map(Anomaly::getProcessingHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(resolved.size()), 2, RoundingMode.HALF_UP);
    }

    private String inspectorName(Anomaly a) {
        if (a.getAcceptanceItem() == null || a.getAcceptanceItem().getAcceptance() == null) {
            return "（批次级异常）";
        }
        User inspector = a.getAcceptanceItem().getAcceptance().getInspector();
        return inspector != null ? inspector.getName() : "未知";
    }
}
