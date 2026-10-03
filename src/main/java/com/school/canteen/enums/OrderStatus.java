package com.school.canteen.enums;

/** 订货单状态 */
public enum OrderStatus {
    DRAFT,       // 草稿（计划生成）
    CONFIRMED,   // 已确认下单
    DELIVERING,  // 部分到货
    COMPLETED,   // 全部验收完成
    CANCELLED
}
