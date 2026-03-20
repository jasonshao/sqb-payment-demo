package com.example.sqbpayment.leaf;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 双 Buffer 号段容器
 * 持有两个 Segment，通过 currentIndex 切换
 */
public class SegmentBuffer {

    private final String bizTag;
    private final Segment[] segments = new Segment[]{new Segment(), new Segment()};
    private volatile int currentIndex = 0;
    private volatile boolean nextReady = false;
    private volatile boolean nextLoading = false;
    private final ReentrantLock lock = new ReentrantLock();

    public SegmentBuffer(String bizTag) {
        this.bizTag = bizTag;
    }

    public String getBizTag() {
        return bizTag;
    }

    public Segment getCurrent() {
        return segments[currentIndex];
    }

    public Segment getNext() {
        return segments[(currentIndex + 1) % 2];
    }

    /**
     * 切换到 next 号段，重置状态
     */
    public void switchToNext() {
        currentIndex = (currentIndex + 1) % 2;
        nextReady = false;
        nextLoading = false;
    }

    public boolean isNextReady() {
        return nextReady;
    }

    public void setNextReady(boolean nextReady) {
        this.nextReady = nextReady;
    }

    public boolean isNextLoading() {
        return nextLoading;
    }

    public void setNextLoading(boolean nextLoading) {
        this.nextLoading = nextLoading;
    }

    public ReentrantLock getLock() {
        return lock;
    }
}
