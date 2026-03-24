package com.example.sqbpayment.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;

class AsyncConfigTest {

    @Test
    void testPollExecutorHasCallerRunsPolicy() {
        AsyncConfig config = new AsyncConfig();
        Executor executor = config.pollExecutor();

        assertInstanceOf(ThreadPoolTaskExecutor.class, executor);
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class,
                taskExecutor.getThreadPoolExecutor().getRejectedExecutionHandler());
    }
}
