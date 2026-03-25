package com.example.sqbpayment.domain.operations;

import com.example.sqbpayment.domain.credential.SqbCredentialProvider;
import com.example.sqbpayment.domain.credential.TerminalCredential;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.request.CancelCommand;
import com.example.sqbpayment.sdk.SqbRequestOptions;
import com.example.sqbpayment.sdk.responsegetter.SqbApiRequest;
import com.example.sqbpayment.sdk.responsegetter.SqbResponseGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 撤单操作默认实现
 */
@Component
public class DefaultCancelOperations implements SqbCancelOperations {

    private static final Logger log = LoggerFactory.getLogger(DefaultCancelOperations.class);

    private final SqbResponseGetter responseGetter;
    private final SqbCredentialProvider credentialProvider;

    public DefaultCancelOperations(SqbResponseGetter responseGetter,
                                   SqbCredentialProvider credentialProvider) {
        this.responseGetter = responseGetter;
        this.credentialProvider = credentialProvider;
    }

    @Override
    public SqbResponse cancel(CancelCommand command, SqbRequestOptions options) {
        command.validate();
        TerminalCredential cred = credentialProvider.getTerminalCredential(null);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("terminal_sn", cred.terminalSn());
        if (command.sn() != null) {
            body.put("sn", command.sn());
        }
        if (command.clientSn() != null) {
            body.put("client_sn", command.clientSn());
        }

        log.info("发起撤单: sn={}, clientSn={}", command.sn(), command.clientSn());
        return responseGetter.request(new SqbApiRequest(
                "/upay/v2/cancel", body, cred.terminalSn(), cred.terminalKey(), options));
    }
}
