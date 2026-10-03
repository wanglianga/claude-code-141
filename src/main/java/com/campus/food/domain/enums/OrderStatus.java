package com.campus.food.domain.enums;

/** 订货单状态 */
public enum OrderStatus {
    PLACED,      // 已下单
    DELIVERED,   // 已到货
    ACCEPTED,    // 已验收
    CLOSED,      // 已结算关闭
    CANCELLED    // 已取消
}
