package com.campus.food.domain.enums;

/** 库存流水类型 */
public enum InventoryTxnType {
    IN,          // 入库
    OUT,         // 出库（领用）
    RETURN_OUT,  // 退货出库
    ADJUST       // 盘点调整
}
