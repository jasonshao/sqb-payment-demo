package com.example.sqbpayment.domain.operations;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.domain.credential.SqbCredentialProvider;
import com.example.sqbpayment.domain.credential.TerminalCredential;
import com.example.sqbpayment.domain.credential.VendorCredential;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.sdk.SqbRequestOptions;
import com.example.sqbpayment.sdk.responsegetter.SqbApiRequest;
import com.example.sqbpayment.sdk.responsegetter.SqbResponseGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 终端管理操作默认实现（激活 + 签到）
 */
@Component
public class DefaultTerminalOperations implements SqbTerminalOperations {

    private static final Logger log = LoggerFactory.getLogger(DefaultTerminalOperations.class);

    private final SqbResponseGetter responseGetter;
    private final SqbCredentialProvider credentialProvider;
    private final SqbConfig config;

    public DefaultTerminalOperations(SqbResponseGetter responseGetter,
                                     SqbCredentialProvider credentialProvider,
                                     SqbConfig config) {
        this.responseGetter = responseGetter;
        this.credentialProvider = credentialProvider;
        this.config = config;
    }

    @Override
    public SqbResponse activate(String code, String deviceId, String name, SqbRequestOptions options) {
        VendorCredential vendorCred = credentialProvider.getVendorCredential();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app_id", config.getAppId());
        body.put("code", code);
        body.put("device_id", deviceId);
        body.put("name", name);

        log.info("发起终端激活: deviceId={}", deviceId);
        SqbResponse response = responseGetter.request(new SqbApiRequest(
                "/terminal/activate", body, vendorCred.vendorSn(), vendorCred.vendorKey(), options));

        if (response.isCommunicationSuccess() && "ACTIVATE_SUCCESS".equals(response.getBizResultCode())) {
            String terminalSn = response.getTerminalSn();
            String terminalKey = response.getTerminalKey();
            log.info("终端激活成功: terminalSn={}", terminalSn);

            TerminalCredential newCred = new TerminalCredential(
                    terminalSn, terminalKey, deviceId, LocalDateTime.now());
            credentialProvider.updateTerminalCredential(deviceId, newCred);
        } else {
            log.error("终端激活失败: {}", response);
        }

        return response;
    }

    @Override
    public SqbResponse checkin(SqbRequestOptions options) {
        String deviceId = config.getDeviceId();
        TerminalCredential cred = credentialProvider.getTerminalCredential(deviceId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("terminal_sn", cred.terminalSn());
        body.put("device_id", deviceId);

        log.info("发起终端签到: terminalSn={}", cred.terminalSn());
        SqbResponse response = responseGetter.request(new SqbApiRequest(
                "/terminal/checkin", body, cred.terminalSn(), cred.terminalKey(), options));

        if (response.isCommunicationSuccess() && "TERMINAL_CHECKIN_SUCCESS".equals(response.getBizResultCode())) {
            String newTerminalKey = response.getTerminalKey();
            log.info("终端签到成功，terminal_key 已更新");

            TerminalCredential newCred = new TerminalCredential(
                    cred.terminalSn(), newTerminalKey, deviceId, LocalDateTime.now());
            credentialProvider.updateTerminalCredential(deviceId, newCred);
        } else {
            log.error("终端签到失败: {}", response);
        }

        return response;
    }
}
