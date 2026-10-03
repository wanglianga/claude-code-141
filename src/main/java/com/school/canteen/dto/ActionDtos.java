package com.school.canteen.dto;

import com.school.canteen.enums.CauseCategory;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ActionDtos {

    /** 降级审批决定 */
    public record DowngradeDecisionRequest(@NotNull Boolean approve, BigDecimal priceRatio, String note) {
    }

    /** 异常处理（归因 + 解决） */
    public record ResolveAnomalyRequest(@NotNull CauseCategory causeCategory, String resolutionNote) {
    }

    /** 发起索赔 */
    public record ClaimRequest(@NotNull Long anomalyId, @NotNull BigDecimal amount, @NotNull String reason) {
    }

    /** 索赔审批 */
    public record ClaimDecisionRequest(@NotNull Boolean approve, String note) {
    }

    /** 生成结算单 */
    public record SettlementGenerateRequest(
            @NotNull Long supplierId,
            @NotNull LocalDate periodStart,
            @NotNull LocalDate periodEnd,
            BigDecimal supplierInvoiceAmount) {
    }

    /** 库存出库（菜单领用/损耗） */
    public record ConsumeRequest(@NotNull List<Line> lines, String note) {
        public record Line(@NotNull Long inventoryItemId, @NotNull BigDecimal quantity) {
        }
    }

    /** 调整安全库存线 */
    public record SafetyStockRequest(@NotNull BigDecimal safetyStock) {
    }

    /** 家长公告 */
    public record AnnouncementRequest(
            @NotNull String title,
            @NotNull String content,
            LocalDate mealDate,
            String mealType,
            String dishName,
            Long anomalyId) {
    }
}
