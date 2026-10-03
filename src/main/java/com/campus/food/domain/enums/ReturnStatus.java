package com.campus.food.domain.enums;

/** 退货单状态 */
public enum ReturnStatus {
    PENDING,     // 待供应商取回
    COMPLETED,   // 已退货
    CANCELLED    // 已取消（如降级审批转退货后又被处理）
}
