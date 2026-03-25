package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.enums.OrderStatus;
import com.example.sqbpayment.model.request.PayCommand;
import com.example.sqbpayment.model.request.PayRequest;
import com.example.sqbpayment.util.ClientSnGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 付款码支付服务（B扫C）
 */
@Service
public class SqbPayService {

    private static final Logger log = LoggerFactory.getLogger(SqbPayService.class);

    private final SqbConfig config;
    private final SqbApiTemplate apiTemplate;
    private final SqbQueryService queryService;
    private final ClientSnGenerator clientSnGenerator;

    public SqbPayService(SqbConfig config, SqbApiTemplate apiTemplate, SqbQueryService queryService,
                         ClientSnGenerator clientSnGenerator) {
        this.config = config;
        this.apiTemplate = apiTemplate;
        this.queryService = queryService;
        this.clientSnGenerator = clientSnGenerator;
    }

    public CompletableFuture<SqbResponse> pay(PayCommand command) {
        String clientSn = clientSnGenerator.generate();

        PayRequest request = new PayRequest();
        request.setTerminalSn(config.getTerminalSn());
        request.setClientSn(clientSn);
        request.setTotalAmount(String.valueOf(command.totalAmount()));
        request.setDynamicId(command.dynamicId());
        request.setSubject(command.subject());
        request.setOperator(command.operator());
        request.setNotifyUrl(command.notifyUrl());

        SqbResponse sqbResponse = apiTemplate.call("/upay/v2/pay", request);

        if (!sqbResponse.isCommunicationSuccess()) {
            log.error("支付请求通信失败: {}", sqbResponse);
            return CompletableFuture.completedFuture(sqbResponse);
        }

        String bizResultCode = sqbResponse.getBizResultCode();

        if ("PAY_FAIL".equals(bizResultCode)) {
            log.info("支付失败: clientSn={}, reason={}", clientSn, sqbResponse.getBizErrorMessage());
            return CompletableFuture.completedFuture(sqbResponse);
        }

        if ("PAY_SUCCESS".equals(bizResultCode)) {
            String orderStatus = sqbResponse.getOrderStatus();
            if (OrderStatus.isFinal(orderStatus)) {
                log.info("支付结果确定: clientSn={}, status={}", clientSn, orderStatus);
                return CompletableFuture.completedFuture(sqbResponse);
            }
        }

        log.info("支付状态未确定，启动异步轮询查询: clientSn={}, bizResultCode={}", clientSn, bizResultCode);
        return queryService.pollByClientSn(clientSn);
    }
}
