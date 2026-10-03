package com.school.canteen.service;

import com.school.canteen.entity.*;
import com.school.canteen.enums.Category;
import com.school.canteen.exception.ApiException;
import com.school.canteen.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** 验收失败影响分析：受影响餐次、替代食材、家长公告、供应商责任 */
@Service
public class ImpactService {

    private final MenuIngredientRepository menuIngredientRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SupplierPriceRepository priceRepository;
    private final AcceptanceRepository acceptanceRepository;
    private final AnomalyRepository anomalyRepository;
    private final ParentAnnouncementRepository announcementRepository;

    public ImpactService(MenuIngredientRepository menuIngredientRepository,
                         InventoryItemRepository inventoryItemRepository,
                         SupplierPriceRepository priceRepository,
                         AcceptanceRepository acceptanceRepository,
                         AnomalyRepository anomalyRepository,
                         ParentAnnouncementRepository announcementRepository) {
        this.menuIngredientRepository = menuIngredientRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.priceRepository = priceRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.anomalyRepository = anomalyRepository;
        this.announcementRepository = announcementRepository;
    }

    /** 某食材在未来已发布/执行中菜单里被多少餐次使用 */
    public int countUpcomingMeals(String ingredientName) {
        return (int) menuIngredientRepository.findUpcomingUsage(ingredientName, LocalDate.now())
                .stream().map(mi -> mi.getMeal().getId()).distinct().count();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> impactReport(Long acceptanceId) {
        Acceptance acceptance = acceptanceRepository.findById(acceptanceId)
                .orElseThrow(() -> ApiException.notFound("验收单不存在: " + acceptanceId));
        Supplier supplier = acceptance.getBatch().getOrder().getSupplier();

        // 验收失败/降级的食材
        List<AcceptanceItem> failed = acceptance.getItems().stream()
                .filter(ai -> ai.getRejectedQty().compareTo(BigDecimal.ZERO) > 0
                        || ai.getDowngradedQty().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        List<Map<String, Object>> affectedMeals = new ArrayList<>();
        List<Map<String, Object>> substitutes = new ArrayList<>();
        Set<String> seenSubstitute = new HashSet<>();

        for (AcceptanceItem ai : failed) {
            OrderItem oi = ai.getOrderItem();
            BigDecimal shortage = ai.getRejectedQty().add(ai.getDowngradedQty());
            BigDecimal available = inventoryItemRepository
                    .findByIngredientNameAndSpecAndUnit(oi.getIngredientName(), oi.getSpec(), oi.getUnit())
                    .map(InventoryItem::getQuantity).orElse(BigDecimal.ZERO);

            List<MenuIngredient> usages = menuIngredientRepository
                    .findUpcomingUsage(oi.getIngredientName(), LocalDate.now());
            for (MenuIngredient mi : usages) {
                MenuMeal meal = mi.getMeal();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("mealId", meal.getId());
                m.put("mealDate", meal.getMealDate());
                m.put("mealType", meal.getMealType());
                m.put("dishName", meal.getDishName());
                m.put("ingredient", oi.getIngredientName());
                m.put("requiredQty", mi.getQuantity() + mi.getUnit());
                m.put("shortageQty", shortage + oi.getUnit());
                m.put("stockAvailable", available + oi.getUnit());
                m.put("risk", available.compareTo(mi.getQuantity()) < 0 ? "库存不足，需替代或补货" : "库存可覆盖");
                affectedMeals.add(m);
            }

            // 替代食材：同品类库存高于安全线的其他食材
            if (seenSubstitute.add(oi.getCategory().name())) {
                List<Map<String, Object>> stockSubs = inventoryItemRepository.findAll().stream()
                        .filter(it -> it.getCategory() == oi.getCategory()
                                && !it.getIngredientName().equals(oi.getIngredientName())
                                && it.getQuantity().compareTo(it.getSafetyStock()) > 0)
                        .limit(5)
                        .map(it -> {
                            Map<String, Object> s = new LinkedHashMap<>();
                            s.put("type", "库存替代");
                            s.put("ingredient", it.getIngredientName());
                            s.put("available", it.getQuantity() + it.getUnit());
                            return s;
                        }).collect(Collectors.toList());
                substitutes.addAll(stockSubs);
            }
            // 替代货源：其他供应商的同食材报价
            priceRepository.findByIngredientName(oi.getIngredientName()).stream()
                    .filter(p -> !p.getSupplier().getId().equals(supplier.getId()))
                    .filter(p -> p.getSupplier().getStatus() == Supplier.Status.ACTIVE)
                    .forEach(p -> {
                        Map<String, Object> s = new LinkedHashMap<>();
                        s.put("type", "替代货源");
                        s.put("supplier", p.getSupplier().getName());
                        s.put("ingredient", p.getIngredientName());
                        s.put("price", p.getPrice() + "元/" + p.getUnit());
                        substitutes.add(s);
                    });
        }

        // 供应商责任：本次验收产生的异常与建议索赔
        List<Anomaly> anomalies = anomalyRepository.findByBatchId(acceptance.getBatch().getId());
        List<Map<String, Object>> responsibility = anomalies.stream().map(a -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("anomalyId", a.getId());
            r.put("type", a.getType());
            r.put("severity", a.getSeverity());
            r.put("description", a.getDescription());
            r.put("penaltyAmount", a.getPenaltyAmount());
            r.put("claimSuggestedAmount", a.getClaimSuggestedAmount());
            r.put("status", a.getStatus());
            return r;
        }).collect(Collectors.toList());

        // 已生成的家长公告
        List<ParentAnnouncement> announcements = announcementRepository.findAll().stream()
                .filter(pa -> pa.getAnomaly() != null
                        && anomalies.stream().anyMatch(a -> a.getId().equals(pa.getAnomaly().getId())))
                .toList();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("acceptanceId", acceptanceId);
        report.put("supplier", supplier.getName());
        report.put("failedItems", failed.stream().map(ai -> {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("ingredient", ai.getOrderItem().getIngredientName());
            f.put("category", ai.getOrderItem().getCategory());
            f.put("rejectedQty", ai.getRejectedQty());
            f.put("downgradedQty", ai.getDowngradedQty());
            f.put("unit", ai.getOrderItem().getUnit());
            f.put("reason", ai.getRejectReason());
            return f;
        }).collect(Collectors.toList()));
        report.put("affectedMeals", affectedMeals);
        report.put("substitutes", substitutes);
        report.put("supplierResponsibility", responsibility);
        report.put("parentAnnouncements", announcements);
        return report;
    }
}
