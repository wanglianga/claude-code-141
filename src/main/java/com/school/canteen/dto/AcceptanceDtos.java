package com.school.canteen.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class AcceptanceDtos {

    /** 发起验收 */
    public record StartRequest(@NotNull Long batchId, String notes) {
    }

    /** 单个订货明细的验收结果 */
    public record ItemRequest(
            @NotNull Long orderItemId,
            @NotNull BigDecimal deliveredQty,
            @NotNull BigDecimal acceptedQty,
            BigDecimal rejectedQty,
            BigDecimal downgradedQty,
            Boolean temperatureOk,
            Boolean appearanceOk,
            Boolean shelfLifeOk,
            Boolean samplingOk,
            BigDecimal rotRatio,
            Boolean substituted,
            /** 食堂已备餐无法拒收：true 时 rejectedQty 部分按 forcedDiscountRatio 折价收货 */
            Boolean forceAccepted,
            BigDecimal forcedDiscountRatio,
            String rejectReason) {
    }

    /** 完成验收 */
    public record CompleteRequest(@NotEmpty @Valid List<ItemRequest> items, String notes) {
    }
}
