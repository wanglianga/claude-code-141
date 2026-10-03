package com.school.canteen.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/** 业务单号生成器：前缀 + 时间戳 + 序列，如 PO20261003083000-0001 */
@Component
public class NoGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final AtomicInteger seq = new AtomicInteger(0);

    public String next(String prefix) {
        int n = seq.updateAndGet(v -> (v + 1) % 10000);
        return prefix + LocalDateTime.now().format(FMT) + "-" + String.format("%04d", n);
    }
}
