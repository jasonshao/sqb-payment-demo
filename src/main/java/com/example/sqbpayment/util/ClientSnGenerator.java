package com.example.sqbpayment.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 商户订单号生成器，保证全局唯一
 * 格式：yyyyMMddHHmmss + 6位自增序列号
 */
public final class ClientSnGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    private ClientSnGenerator() {
    }

    public static String generate() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        long seq = SEQUENCE.incrementAndGet() % 1000000;
        return timestamp + String.format("%06d", seq);
    }

    public static String generateRefundNo() {
        return "REF" + generate();
    }
}
