package com.example.sqbpayment.scheduler;

import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.service.SqbTerminalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 定时签到调度器
 * 每天自动执行终端签到，更新 terminal_key
 */
@Component
public class CheckinScheduler {

    private static final Logger log = LoggerFactory.getLogger(CheckinScheduler.class);

    private final SqbTerminalService terminalService;

    public CheckinScheduler(SqbTerminalService terminalService) {
        this.terminalService = terminalService;
    }

    @Scheduled(cron = "${sqb.checkin-cron:0 5 0 * * ?}")
    public void scheduledCheckin() {
        try {
            SqbResponse response = terminalService.checkin();
            if (response.isCommunicationSuccess()
                    && "TERMINAL_CHECKIN_SUCCESS".equals(response.getBizResultCode())) {
                log.info("定时签到成功");
            } else {
                log.error("定时签到失败: {}", response);
            }
        } catch (IOException e) {
            log.error("定时签到异常", e);
        }
    }
}
