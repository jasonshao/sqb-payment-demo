package com.example.sqbpayment.scheduler;

import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.service.SqbTerminalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckinSchedulerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private SqbTerminalService terminalService;

    @InjectMocks
    private CheckinScheduler checkinScheduler;

    @Test
    void testScheduledCheckinSuccess() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"TERMINAL_CHECKIN_SUCCESS",
                        "data":{"terminal_key":"new_key"}
                    }
                }
                """;
        when(terminalService.checkin()).thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        assertDoesNotThrow(() -> checkinScheduler.scheduledCheckin());
        verify(terminalService).checkin();
    }

    @Test
    void testScheduledCheckinFailure() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"TERMINAL_CHECKIN_FAIL",
                        "error_message":"签到失败"
                    }
                }
                """;
        when(terminalService.checkin()).thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        assertDoesNotThrow(() -> checkinScheduler.scheduledCheckin());
        verify(terminalService).checkin();
    }

    @Test
    void testScheduledCheckinIOException() throws Exception {
        when(terminalService.checkin()).thenThrow(new IOException("网络异常"));

        assertDoesNotThrow(() -> checkinScheduler.scheduledCheckin());
        verify(terminalService).checkin();
    }
}
