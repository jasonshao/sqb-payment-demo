package com.example.sqbpayment.leaf;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 号段：表示一段可用的 ID 范围 [min, max)
 */
public class Segment {

    private final AtomicLong value = new AtomicLong(0);
    private long min;
    private long max;

    public AtomicLong getValue() {
        return value;
    }

    public long getMin() {
        return min;
    }

    public void setMin(long min) {
        this.min = min;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    /**
     * 剩余可用 ID 数
     */
    public long getRemaining() {
        return max - value.get();
    }
}
