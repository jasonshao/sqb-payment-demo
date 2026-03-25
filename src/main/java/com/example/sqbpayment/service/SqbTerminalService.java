package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.credential.TerminalCredentialEntity;
import com.example.sqbpayment.credential.TerminalCredentialRepository;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.request.ActivateRequest;
import com.example.sqbpayment.model.request.CheckinRequest;
import com.example.sqbpayment.sdk.exception.SqbException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 终端管理服务：激活 + 签到
 */
@Service
public class SqbTerminalService {

    private static final Logger log = LoggerFactory.getLogger(SqbTerminalService.class);

    private final SqbConfig config;
    private final SqbApiTemplate apiTemplate;
    private final TerminalCredentialRepository credentialRepository;

    public SqbTerminalService(SqbConfig config, SqbApiTemplate apiTemplate,
                              TerminalCredentialRepository credentialRepository) {
        this.config = config;
        this.apiTemplate = apiTemplate;
        this.credentialRepository = credentialRepository;
    }

    @PostConstruct
    void loadCredentials() {
        String deviceId = config.getDeviceId();
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }
        credentialRepository.findById(deviceId).ifPresent(entity -> {
            if (entity.getTerminalSn() != null && !entity.getTerminalSn().isBlank()) {
                config.setTerminalSn(entity.getTerminalSn());
                config.setTerminalKey(entity.getTerminalKey());
                log.info("从数据库加载终端凭证: device_id={}, terminal_sn={}", deviceId, entity.getTerminalSn());
            }
        });
    }

    private void persistCredentials(String terminalSn, String terminalKey) {
        String deviceId = config.getDeviceId();
        if (deviceId == null || deviceId.isBlank()) {
            log.warn("device_id 未配置，无法持久化终端凭证");
            return;
        }
        TerminalCredentialEntity entity = new TerminalCredentialEntity();
        entity.setDeviceId(deviceId);
        entity.setTerminalSn(terminalSn);
        entity.setTerminalKey(terminalKey);
        entity.setUpdateTime(LocalDateTime.now());
        credentialRepository.save(entity);
        log.info("终端凭证已持久化: device_id={}", deviceId);
    }

    public SqbResponse activate(String code, String deviceId, String name) {
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

            config.setTerminalSn(terminalSn);
            config.setTerminalKey(terminalKey);
            persistCredentials(terminalSn, terminalKey);
        } else {
            log.error("终端激活失败: {}", sqbResponse);
        }

        return sqbResponse;
    }

    public synchronized SqbResponse checkin() {
        String oldKey = config.getTerminalKey();

        CheckinRequest request = new CheckinRequest();
        request.setTerminalSn(config.getTerminalSn());
        request.setDeviceId(config.getDeviceId());

        SqbResponse sqbResponse;
        try {
            sqbResponse = apiTemplate.call("/terminal/checkin", request);
        } catch (SqbException e) {
            log.warn("签到通信失败，保留旧 terminal_key", e);
            config.setTerminalKey(oldKey);
            throw e;
        }

        if (sqbResponse.isCommunicationSuccess() && "TERMINAL_CHECKIN_SUCCESS".equals(sqbResponse.getBizResultCode())) {
            String newTerminalKey = sqbResponse.getTerminalKey();
            log.info("终端签到成功，terminal_key 已更新");
            config.setTerminalKey(newTerminalKey);
            persistCredentials(config.getTerminalSn(), newTerminalKey);
        } else {
            log.error("终端签到失败: {}", sqbResponse);
        }

        return sqbResponse;
    }
}
