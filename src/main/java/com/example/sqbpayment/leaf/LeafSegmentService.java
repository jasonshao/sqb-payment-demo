package com.example.sqbpayment.leaf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Leaf-segment 号段服务
 *
 * 核心设计：
 * 1. 双 Buffer：current 和 next 两个号段，current 用完无缝切换到 next
 * 2. 异步预加载：current 使用量超过阈值（90%已用）时，异步从 DB 加载 next 号段
 * 3. 线程安全：ReentrantLock 保护号段切换，AtomicLong 保证取号原子性
 */
@Service
public class LeafSegmentService {

    private static final Logger log = LoggerFactory.getLogger(LeafSegmentService.class);

    /**
     * 预加载阈值：当 current 号段使用超过 90% 时触发 next 的异步加载
     */
    private static final double LOAD_THRESHOLD = 0.9;

    private final LeafAllocRepository leafAllocRepository;

    /**
     * 每个 bizTag 拥有独立的 SegmentBuffer
     */
    private final Map<String, SegmentBuffer> bufferMap = new ConcurrentHashMap<>();

    private final ExecutorService loadExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "leaf-segment-loader");
        t.setDaemon(true);
        return t;
    });

    public LeafSegmentService(LeafAllocRepository leafAllocRepository) {
        this.leafAllocRepository = leafAllocRepository;
    }

    /**
     * 获取下一个 ID
     *
     * @param bizTag 业务标识（PAY / REFUND）
     * @return 全局唯一的递增 ID
     */
    public long getNextId(String bizTag) {
        SegmentBuffer buffer = bufferMap.computeIfAbsent(bizTag, k -> {
            SegmentBuffer buf = new SegmentBuffer(k);
            loadSegmentFromDb(buf, buf.getCurrent());
            return buf;
        });

        return getIdFromBuffer(buffer);
    }

    private long getIdFromBuffer(SegmentBuffer buffer) {
        ReentrantLock lock = buffer.getLock();

        // 快路径：尝试从 current 取号
        if (!lock.tryLock()) {
            lock.lock();
        }
        try {
            Segment current = buffer.getCurrent();
            long value = current.getValue().getAndIncrement();

            if (value < current.getMax()) {
                // current 号段未用完
                triggerAsyncLoadIfNeeded(buffer, current);
                return value;
            }

            // current 用尽，尝试切换到 next
            if (buffer.isNextReady()) {
                buffer.switchToNext();
                Segment switched = buffer.getCurrent();
                long newValue = switched.getValue().getAndIncrement();
                triggerAsyncLoadIfNeeded(buffer, switched);
                return newValue;
            }

            // next 也没准备好，同步加载（阻塞）
            log.warn("号段双 Buffer 均不可用，同步加载: bizTag={}", buffer.getBizTag());
            loadSegmentFromDb(buffer, buffer.getNext());
            buffer.switchToNext();
            Segment switched = buffer.getCurrent();
            return switched.getValue().getAndIncrement();

        } finally {
            lock.unlock();
        }
    }

    /**
     * 判断是否需要异步预加载 next 号段
     */
    private void triggerAsyncLoadIfNeeded(SegmentBuffer buffer, Segment current) {
        long used = current.getValue().get() - current.getMin();
        long total = current.getMax() - current.getMin();
        double usageRatio = (double) used / total;

        if (usageRatio >= LOAD_THRESHOLD && !buffer.isNextReady() && !buffer.isNextLoading()) {
            buffer.setNextLoading(true);
            loadExecutor.submit(() -> {
                try {
                    loadSegmentFromDb(buffer, buffer.getNext());
                    buffer.setNextReady(true);
                    log.debug("异步预加载号段完成: bizTag={}", buffer.getBizTag());
                } catch (Exception e) {
                    log.error("异步预加载号段失败: bizTag={}", buffer.getBizTag(), e);
                } finally {
                    buffer.setNextLoading(false);
                }
            });
        }
    }

    /**
     * 从数据库加载一个号段到指定 Segment
     */
    void loadSegmentFromDb(SegmentBuffer buffer, Segment segment) {
        String bizTag = buffer.getBizTag();

        // 原子更新 max_id = max_id + step
        int updated = leafAllocRepository.updateMaxId(bizTag);
        if (updated == 0) {
            throw new IllegalStateException("号段分配失败，biz_tag 不存在: " + bizTag);
        }

        // 读取更新后的值
        LeafAllocEntity entity = leafAllocRepository.findById(bizTag)
                .orElseThrow(() -> new IllegalStateException("号段记录不存在: " + bizTag));

        long maxId = entity.getMaxId();
        int step = entity.getStep();

        // 号段范围: [maxId - step + 1, maxId + 1)  即 maxId - step + 1 到 maxId（含）
        segment.setMin(maxId - step);
        segment.setMax(maxId);
        segment.getValue().set(maxId - step);

        log.info("加载号段: bizTag={}, range=[{}, {}), step={}",
                bizTag, segment.getMin(), segment.getMax(), step);
    }
}
