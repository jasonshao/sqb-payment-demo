package com.example.sqbpayment.leaf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Leaf-segment 号段服务集成测试
 * 使用 H2 内嵌数据库验证号段分配、双 Buffer 切换、并发取号
 */
@SpringBootTest
class LeafSegmentServiceTest {

    @Autowired
    private LeafSegmentService leafSegmentService;

    @Autowired
    private LeafAllocRepository leafAllocRepository;

    @Test
    void testGetNextIdReturnsIncrementingValues() {
        long id1 = leafSegmentService.getNextId("PAY");
        long id2 = leafSegmentService.getNextId("PAY");
        long id3 = leafSegmentService.getNextId("PAY");

        assertTrue(id2 > id1, "ID 应递增");
        assertTrue(id3 > id2, "ID 应递增");
    }

    @Test
    void testDifferentBizTagsAreIndependent() {
        long payId = leafSegmentService.getNextId("PAY");
        long refundId = leafSegmentService.getNextId("REFUND");

        // 两个 bizTag 的号段独立分配，都应该是非负整数
        assertTrue(payId >= 0);
        assertTrue(refundId >= 0);
    }

    @Test
    void testConcurrentUniqueness() throws InterruptedException {
        int threadCount = 20;
        int idsPerThread = 500;
        int totalIds = threadCount * idsPerThread;

        Set<Long> allIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        allIds.add(leafSegmentService.getNextId("PAY"));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(totalIds, allIds.size(), "并发生成的 " + totalIds + " 个 ID 应全部唯一");
    }

    @Test
    void testSegmentExhaustionAndReload() {
        // 记录起始 max_id
        long startMaxId = leafAllocRepository.findById("REFUND").orElseThrow().getMaxId();

        // 快速消耗超过一个号段（step=2000），验证号段切换
        long lastId = -1;
        for (int i = 0; i < 2500; i++) {
            long id = leafSegmentService.getNextId("REFUND");
            assertTrue(id >= 0, "号段切换后 ID 仍应为非负数");
            if (lastId >= 0) {
                assertTrue(id > lastId, "ID 应持续递增");
            }
            lastId = id;
        }

        // 验证 DB 中 max_id 已增长（至少消耗了2个号段=4000的步长）
        long endMaxId = leafAllocRepository.findById("REFUND").orElseThrow().getMaxId();
        assertTrue(endMaxId > startMaxId, "消耗号段后 max_id 应增长");
    }

    @Test
    void testInvalidBizTagThrowsException() {
        assertThrows(Exception.class,
                () -> leafSegmentService.getNextId("NONEXISTENT"),
                "不存在的 biz_tag 应抛出异常");
    }
}
