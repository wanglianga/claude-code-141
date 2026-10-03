package com.school.canteen.enums;

/** 库存流水类型 */
public enum TxnType {
    IN_ACCEPTANCE,   // 验收入库
    IN_DOWNGRADE,    // 降级入库
    OUT_MENU,        // 菜单领用出库
    OUT_RETURN,      // 退货出库
    OUT_LOSS,        // 损耗出库
    ADJUST           // 盘点调整
}
