package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.request.ActivateRequest;
import com.example.sqbpayment.model.request.CheckinRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 终端管理服务：激活 + 签到
 */
@Service
public class SqbTerminalService {

    private static final Logger log = LoggerFactory.getLogger(SqbTerminalService.class);

    private final SqbConfig config;
    private final SqbApiTemplate apiTemplate;

    public SqbTerminalService(SqbConfig config, SqbApiTemplate apiTemplate) {
        this.config = config;
        this.apiTemplate = apiTemplate;
    }

    /**
     * 终端激活
     * 使用 vendor 级别签名：Authorization: {vendor_sn} {MD5(body + vendor_key)}
     * 激活码只能使用一次，激活成功后必须持久化 terminal_sn 和 terminal_key
     *
     * @param code     激活码
     * @param deviceId 设备唯一标识
     * @param name     终端名称（可选）
     * @return 激活响应，包含 terminal_sn 和 terminal_key
     */
    public SqbResponse activate(String code, String deviceId, String name) throws IOException {
        ActivateRequest request = new ActivateRequest();
        request.setAppId(config.getAppId());
        request.setCode(code);
        request.setDeviceId(deviceId);
        request.setName(name);

        SqbResponse sqbResponse = apiTemplate.callAsVendor("/terminal/activate", request);

        if (sqbResponse.isCommunicationSuccess() && "ACTIVATE_SUCCESS".equals(sqbResponse.getBizResultCode())) {
            String terminalSn = sqbResponse.getTerminalSn();
            String terminalKey = sqbResponse.getTerminalKey();
            log.info("终端激活成功: terminal_sn={}", terminalSn);

            // 更新内存中的配置（生产环境应持久化到数据库）
            config.setTerminalSn(terminalSn);
            config.setTerminalKey(terminalKey);
        } else {
            log.error("终端激活失败: {}", sqbResponse);
        }

        return sqbResponse;
    }

    /**
     * 终端签到
     * 使用 terminal 级别签名
     * 签到成功后 terminal_key 会更新，必须立即持久化新 key
     * 建议每天首次交易前执行签到
     *
     * @return 签到响应，包含新的 terminal_key
     */
    public synchronized SqbResponse checkin() throws IOException {
        CheckinRequest request = new CheckinRequest();
        request.setTerminalSn(config.getTerminalSn());
        request.setDeviceId(config.getDeviceId());

        SqbResponse sqbResponse = apiTemplate.call("/terminal/checkin", request);

        if (sqbResponse.isCommunicationSuccess() && "TERMINAL_CHECKIN_SUCCESS".equals(sqbResponse.getBizResultCode())) {
            String newTerminalKey = sqbResponse.getTerminalKey();
            log.info("终端签到成功，terminal_key 已更新");

            // 关键：签到成功后必须立即更新 terminal_key
            config.setTerminalKey(newTerminalKey);
        } else {
            log.error("终端签到失败: {}", sqbResponse);
        }

        return sqbResponse;
    }
}
