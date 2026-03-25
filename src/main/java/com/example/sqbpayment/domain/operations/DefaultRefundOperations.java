package com.example.sqbpayment.domain.operations;

import com.example.sqbpayment.domain.credential.SqbCredentialProvider;
import com.example.sqbpayment.domain.credential.TerminalCredential;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.request.RefundCommand;
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
 * 退款操作默认实现
 */
@Component
public class DefaultRefundOperations implements SqbRefundOperations {

    private static final Logger log = LoggerFactory.getLogger(DefaultRefundOperations.class);

    private final SqbResponseGetter responseGetter;
    private final SqbCredentialProvider credentialProvider;
    private final ClientSnGenerator clientSnGenerator;

    public DefaultRefundOperations(SqbResponseGetter responseGetter,
                                   SqbCredentialProvider credentialProvider,
                                   ClientSnGenerator clientSnGenerator) {
        this.responseGetter = responseGetter;
        this.credentialProvider = credentialProvider;
        this.clientSnGenerator = clientSnGenerator;
    }

    @Override
    public SqbResponse refund(RefundCommand command, SqbRequestOptions options) {
        command.validate();
        TerminalCredential cred = credentialProvider.getTerminalCredential(null);
        String refundRequestNo = clientSnGenerator.generateRefundNo();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("terminal_sn", cred.terminalSn());
        if (command.sn() != null) {
            body.put("sn", command.sn());
        }
        if (command.clientSn() != null) {
            body.put("client_sn", command.clientSn());
        }
        body.put("refund_request_no", refundRequestNo);
        body.put("refund_amount", String.valueOf(command.refundAmount()));
        body.put("operator", command.operator());
        if (command.refundReason() != null) {
            body.put("refund_reason", command.refundReason());
        }

        log.info("发起退款: refundRequestNo={}", refundRequestNo);
        return responseGetter.request(new SqbApiRequest(
                "/upay/v2/refund", body, cred.terminalSn(), cred.terminalKey(), options));
    }
}
