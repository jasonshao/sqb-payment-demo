package com.example.sqbpayment.util;

import com.example.sqbpayment.leaf.LeafSegmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientSnGeneratorTest {

    @Mock
    private LeafSegmentService leafSegmentService;

    private ClientSnGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ClientSnGenerator(leafSegmentService);
    }

    @Test
    void testGenerateUniqueness() {
        AtomicLong counter = new AtomicLong(1);
        when(leafSegmentService.getNextId("PAY")).thenAnswer(inv -> counter.getAndIncrement());

        Set<String> sns = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            sns.add(generator.generate());
        }
        assertEquals(1000, sns.size(), "生成的 client_sn 应全局唯一");
    }

    @Test
    void testGenerateFormat() {
        when(leafSegmentService.getNextId("PAY")).thenReturn(42L);

        String sn = generator.generate();
        // 8位日期 + 12位号段ID = 20位
        assertEquals(20, sn.length());
        assertTrue(sn.matches("\\d{20}"));
        // 末尾应包含号段ID（左补零至12位）
        assertTrue(sn.endsWith("000000000042"));
    }

    @Test
    void testGenerateRefundNo() {
        when(leafSegmentService.getNextId("REFUND")).thenReturn(99L);

        String refundNo = generator.generateRefundNo();
        assertTrue(refundNo.startsWith("REF"));
        assertEquals(23, refundNo.length());
        assertTrue(refundNo.endsWith("000000000099"));
    }

    @Test
    void testGenerateUsesPayBizTag() {
        when(leafSegmentService.getNextId("PAY")).thenReturn(1L);

        generator.generate();

        verify(leafSegmentService).getNextId("PAY");
        verifyNoMoreInteractions(leafSegmentService);
    }

    @Test
    void testGenerateRefundUsesRefundBizTag() {
        when(leafSegmentService.getNextId("REFUND")).thenReturn(1L);

        generator.generateRefundNo();

        verify(leafSegmentService).getNextId("REFUND");
        verifyNoMoreInteractions(leafSegmentService);
    }
}
