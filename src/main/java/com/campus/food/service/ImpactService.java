package com.campus.food.service;

import com.campus.food.domain.*;
import com.campus.food.domain.enums.*;
import com.campus.food.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 影响分析：某批食材验收失败时，给出受影响餐次、替代食材、家长公告草稿与供应商责任。
 */
@Service
@RequiredArgsConstructor
public class ImpactService {

    private final DeliveryBatchRepo batchRepo;
    private final AcceptanceRecordRepo recordRepo;
    private final AcceptanceItemRepo acceptanceItemRepo;
    private final MenuMealIngredientRepo mealIngredientRepo;
    private final IngredientRepo ingredientRepo;
    private final StockLevelRepo stockLevelRepo;
    private final ParentAnnouncementRepo announcementRepo;
    private final AnomalyRecordRepo anomalyRepo;
    private final ClaimRepo claimRepo;

    @Transactional
    public ImpactReport analyzeBatch(Long batchId) {
        DeliveryBatch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> BusinessException.notFound("送货批次不存在: " + batchId));
        AcceptanceRecord record = recordRepo.findByBatchId(batchId)
                .orElseThrow(() -> BusinessException.badRequest("该批次尚未验收，无法做影响分析"));
        Supplier supplier = batch.getOrder().getSupplier();
        LocalDate from = batch.getArrivedAt().toLocalDate();
        LocalDate to = from.plusDays(7);

        List<IngredientImpact> impacts = new ArrayList<>();
        List<ParentAnnouncement> announcements = new ArrayList<>();

        for (AcceptanceItem ai : acceptanceItemRepo.findByRecordId(record.getId())) {
            BigDecimal pendingDowngrade = ai.getDowngradeStatus() == DowngradeStatus.PENDING
                    ? ai.getDowngradedQty() : BigDecimal.ZERO;
            BigDecimal failedQty = ai.getRejectedQty().add(pendingDowngrade);
            if (failedQty.signum() <= 0) {
                continue;
            }
            Ingredient ing = ai.getIngredient();
            BigDecimal stock = stockLevelRepo.findByIngredientId(ing.getId())
                    .map(StockLevel::getQuantity).orElse(BigDecimal.ZERO);

            // 受影响餐次
            List<AffectedMeal> affectedMeals = new ArrayList<>();
            for (MenuMealIngredient mmi : mealIngredientRepo.findMealsNeeding(ing.getId(), from, to,
                    List.of(MealStatus.SCHEDULED, MealStatus.PREPARED))) {
                MenuMeal meal = mmi.getMeal();
                BigDecimal shortage = mmi.getQuantity().subtract(stock).max(BigDecimal.ZERO);
                affectedMeals.add(new AffectedMeal(meal.getId(), meal.getDate(), meal.getMealType(),
                        meal.getDishName(), meal.getStatus(), mmi.getQuantity(), shortage));
                announcements.add(ensureAnnouncement(meal, ing, supplier));
            }

            // 替代食材：优先同替代组，无组或组内无候选则退化为同品类，需有库存
            List<SubstituteOption> substitutes = new ArrayList<>();
            List<Ingredient> candidates = new ArrayList<>();
            if (ing.getSubstituteGroup() != null && !ing.getSubstituteGroup().isBlank()) {
                candidates.addAll(ingredientRepo.findBySubstituteGroup(ing.getSubstituteGroup()));
            }
            candidates.removeIf(c -> c.getId().equals(ing.getId()));
            if (candidates.isEmpty()) {
                candidates = ingredientRepo.findByCategory(ing.getCategory());
            }
            for (Ingredient c : candidates) {
                if (c.getId().equals(ing.getId())) {
                    continue;
                }
                BigDecimal cStock = stockLevelRepo.findByIngredientId(c.getId())
                        .map(StockLevel::getQuantity).orElse(BigDecimal.ZERO);
                if (cStock.signum() > 0) {
                    substitutes.add(new SubstituteOption(c.getId(), c.getName(), c.getUnit(), cStock));
                }
            }

            impacts.add(new IngredientImpact(ing.getId(), ing.getName(), ing.getCategory(),
                    failedQty, stock, stock.compareTo(ing.getSafetyStock()) < 0,
                    affectedMeals, substitutes));
        }

        // 供应商责任
        List<AnomalyRecord> anomalies = anomalyRepo.findByBatchId(batchId);
        List<Claim> claims = claimRepo.findByBatchId(batchId);
        BigDecimal claimTotal = claims.stream()
                .filter(c -> c.getStatus() == ClaimStatus.PENDING || c.getStatus() == ClaimStatus.APPROVED)
                .map(Claim::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        SupplierResponsibility responsibility = new SupplierResponsibility(
                supplier.getId(), supplier.getName(),
                anomalies.stream().map(a -> a.getAnomalyNo() + " " + a.getType() + "(" + a.getStatus() + ")").toList(),
                claims.stream().map(c -> c.getClaimNo() + " " + c.getAmount() + "(" + c.getStatus() + ")").toList(),
                claimTotal);

        return new ImpactReport(batch.getId(), batch.getBatchNo(), record.getRecordNo(),
                impacts, announcements, responsibility);
    }

    @Transactional
    public ImpactReport analyzeAcceptanceItem(Long acceptanceItemId) {
        AcceptanceItem ai = acceptanceItemRepo.findById(acceptanceItemId)
                .orElseThrow(() -> BusinessException.notFound("验收明细不存在: " + acceptanceItemId));
        return analyzeBatch(ai.getRecord().getBatch().getId());
    }

    /** 为受影响餐次生成家长公告草稿（已存在则复用） */
    private ParentAnnouncement ensureAnnouncement(MenuMeal meal, Ingredient ing, Supplier supplier) {
        List<ParentAnnouncement> existing = announcementRepo.findByMealId(meal.getId()).stream()
                .filter(a -> a.getStatus() != AnnouncementStatus.REVOKED)
                .toList();
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        ParentAnnouncement a = new ParentAnnouncement();
        a.setMeal(meal);
        a.setTitle("【供餐调整通知】" + meal.getDate() + " " + meal.getMealType() + " " + meal.getDishName());
        a.setContent("各位家长：因食材[" + ing.getName() + "]验收不合格（供应商：" + supplier.getName()
                + "），" + meal.getDate() + " " + meal.getMealType() + " 菜品「" + meal.getDishName()
                + "」将使用合格替代食材或调整菜品，学生用餐安全不受影响。特此告知。");
        a.setReason("食材验收不合格");
        return announcementRepo.save(a);
    }

    public record AffectedMeal(Long mealId, LocalDate date, MealType mealType, String dishName,
                               MealStatus status, BigDecimal requiredQty, BigDecimal shortageQty) {
    }

    public record SubstituteOption(Long ingredientId, String name, String unit, BigDecimal availableQty) {
    }

    public record IngredientImpact(Long ingredientId, String name, IngredientCategory category,
                                   BigDecimal failedQty, BigDecimal currentStock, boolean belowSafetyLine,
                                   List<AffectedMeal> affectedMeals, List<SubstituteOption> substitutes) {
    }

    public record SupplierResponsibility(Long supplierId, String supplierName,
                                         List<String> anomalies, List<String> claims,
                                         BigDecimal openClaimAmount) {
    }

    public record ImpactReport(Long batchId, String batchNo, String recordNo,
                               List<IngredientImpact> impacts,
                               List<ParentAnnouncement> announcements,
                               SupplierResponsibility supplierResponsibility) {
    }
}
