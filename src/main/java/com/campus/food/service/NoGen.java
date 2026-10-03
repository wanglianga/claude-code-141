package com.campus.food.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/** 业务单号生成器 */
public final class NoGen {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private NoGen() {
    }

    public static String next(String prefix) {
        int n = SEQ.incrementAndGet() % 10000;
        return prefix + LocalDateTime.now().format(FMT) + String.format("-%04d", n);
    }
}
