package com.example.sqbpayment.util;

import com.example.sqbpayment.leaf.LeafSegmentService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 商户订单号生成器，基于 Leaf-segment 号段模式保证全局唯一
 *
 * 格式：yyyyMMdd + 12位号段ID（左补零）= 20位
 * 退款格式：REF + yyyyMMdd + 12位号段ID = 23位
 *
 * 日期用于可读性，ID 唯一性完全由号段保证
 */
@Component
public class ClientSnGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String BIZ_TAG_PAY = "PAY";
    private static final String BIZ_TAG_REFUND = "REFUND";

    private final LeafSegmentService leafSegmentService;

    public ClientSnGenerator(LeafSegmentService leafSegmentService) {
        this.leafSegmentService = leafSegmentService;
    }

    /**
     * 生成支付订单号
     * 格式：yyyyMMdd + 12位号段序列号 = 20位纯数字
     */
    public String generate() {
        String date = LocalDate.now().format(DATE_FORMATTER);
        long id = leafSegmentService.getNextId(BIZ_TAG_PAY);
        return date + String.format("%012d", id);
    }

    /**
     * 生成退款请求号
     * 格式：REF + yyyyMMdd + 12位号段序列号 = 23位
     */
    public String generateRefundNo() {
        String date = LocalDate.now().format(DATE_FORMATTER);
        long id = leafSegmentService.getNextId(BIZ_TAG_REFUND);
        return "REF" + date + String.format("%012d", id);
    }
}
