package com.example.sqbpayment.domain.operations;

import com.example.sqbpayment.domain.credential.SqbCredentialProvider;
import com.example.sqbpayment.domain.credential.TerminalCredential;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.sdk.SqbRequestOptions;
import com.example.sqbpayment.sdk.responsegetter.SqbApiRequest;
import com.example.sqbpayment.sdk.responsegetter.SqbResponseGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 查询操作默认实现
 */
@Component
public class DefaultQueryOperations implements SqbQueryOperations {

    private static final Logger log = LoggerFactory.getLogger(DefaultQueryOperations.class);

    private final SqbResponseGetter responseGetter;
    private final SqbCredentialProvider credentialProvider;

    public DefaultQueryOperations(SqbResponseGetter responseGetter,
                                  SqbCredentialProvider credentialProvider) {
        this.responseGetter = responseGetter;
        this.credentialProvider = credentialProvider;
    }

    @Override
    public SqbResponse queryByClientSn(String clientSn, SqbRequestOptions options) {
        TerminalCredential cred = credentialProvider.getTerminalCredential(null);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("terminal_sn", cred.terminalSn());
        body.put("client_sn", clientSn);

        log.info("按商户订单号查询: clientSn={}", clientSn);
        return responseGetter.request(new SqbApiRequest(
                "/upay/v2/query", body, cred.terminalSn(), cred.terminalKey(), options));
    }

    @Override
    public SqbResponse queryBySn(String sn, SqbRequestOptions options) {
        TerminalCredential cred = credentialProvider.getTerminalCredential(null);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("terminal_sn", cred.terminalSn());
        body.put("sn", sn);

        log.info("按收钱吧订单号查询: sn={}", sn);
        return responseGetter.request(new SqbApiRequest(
                "/upay/v2/query", body, cred.terminalSn(), cred.terminalKey(), options));
    }
}
