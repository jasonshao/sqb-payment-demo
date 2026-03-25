package com.example.sqbpayment.domain.operations;

import com.example.sqbpayment.domain.credential.SqbCredentialProvider;
import com.example.sqbpayment.domain.credential.TerminalCredential;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.request.PayCommand;
import com.example.sqbpayment.model.request.PrecreateCommand;
import com.example.sqbpayment.sdk.SqbRequestOptions;
import com.example.sqbpayment.sdk.responsegetter.SqbApiRequest;
import com.example.sqbpayment.sdk.responsegetter.SqbResponseGetter;
import com.example.sqbpayment.util.ClientSnGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付操作默认实现
 */
@Component
public class DefaultPaymentOperations implements SqbPaymentOperations {

    private static final Logger log = LoggerFactory.getLogger(DefaultPaymentOperations.class);

    private final SqbResponseGetter responseGetter;
    private final SqbCredentialProvider credentialProvider;
    private final ClientSnGenerator clientSnGenerator;

    public DefaultPaymentOperations(SqbResponseGetter responseGetter,
                                    SqbCredentialProvider credentialProvider,
                                    ClientSnGenerator clientSnGenerator) {
        this.responseGetter = responseGetter;
        this.credentialProvider = credentialProvider;
        this.clientSnGenerator = clientSnGenerator;
    }

    @Override
    public SqbResponse pay(PayCommand command, SqbRequestOptions options) {
        TerminalCredential cred = credentialProvider.getTerminalCredential(null);
        String clientSn = clientSnGenerator.generate();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("terminal_sn", cred.terminalSn());
        body.put("client_sn", clientSn);
        body.put("total_amount", String.valueOf(command.totalAmount()));
        body.put("dynamic_id", command.dynamicId());
        body.put("subject", command.subject());
        body.put("operator", command.operator());
        if (command.notifyUrl() != null) {
            body.put("notify_url", command.notifyUrl());
        }

        log.info("发起付款码支付: clientSn={}", clientSn);
        return responseGetter.request(new SqbApiRequest(
                "/upay/v2/pay", body, cred.terminalSn(), cred.terminalKey(), options));
    }

    @Override
    public SqbResponse precreate(PrecreateCommand command, SqbRequestOptions options) {
        TerminalCredential cred = credentialProvider.getTerminalCredential(null);
        String clientSn = clientSnGenerator.generate();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("terminal_sn", cred.terminalSn());
        body.put("client_sn", clientSn);
        body.put("total_amount", String.valueOf(command.totalAmount()));
        body.put("payway", command.payway());
        body.put("subject", command.subject());
        body.put("operator", command.operator());
        if (command.notifyUrl() != null) {
            body.put("notify_url", command.notifyUrl());
        }

        log.info("发起预创建支付: clientSn={}", clientSn);
        return responseGetter.request(new SqbApiRequest(
                "/upay/v2/precreate", body, cred.terminalSn(), cred.terminalKey(), options));
    }
}
