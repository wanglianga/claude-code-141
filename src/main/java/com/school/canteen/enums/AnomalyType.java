package com.school.canteen.enums;

/** 异常类型 */
public enum AnomalyType {
    SHORT_DELIVERY,       // 少送
    LATE_DELIVERY,        // 晚送
    COLD_CHAIN_BREACH,    // 冷链温度超标
    HIGH_ROT_RATIO,       // 腐烂比例过高
    MISSING_CERTIFICATE,  // 票证缺失
    SUBSTITUTION,         // 临时替换品类
    FORCED_ACCEPTANCE,    // 食堂已备餐无法拒收
    WEIGHT_MISMATCH,      // 送货与验收重量不一致
    SETTLEMENT_MISMATCH,  // 结算金额与验收重量不一致
    QUALITY_FAILURE       // 其他质检不合格
}
