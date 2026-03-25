package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.enums.OrderStatus;
import com.example.sqbpayment.model.request.PrecreateCommand;
import com.example.sqbpayment.model.request.PrecreateRequest;
import com.example.sqbpayment.util.ClientSnGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 预创建支付服务（C2B / 客扫商户码）
 */
@Service
public class SqbPrecreateService {

    private static final Logger log = LoggerFactory.getLogger(SqbPrecreateService.class);

    private final SqbConfig config;
    private final SqbApiTemplate apiTemplate;
    private final SqbQueryService queryService;
    private final ClientSnGenerator clientSnGenerator;

    public SqbPrecreateService(SqbConfig config, SqbApiTemplate apiTemplate, SqbQueryService queryService,
                               ClientSnGenerator clientSnGenerator) {
        this.config = config;
        this.apiTemplate = apiTemplate;
        this.queryService = queryService;
        this.clientSnGenerator = clientSnGenerator;
    }

    public CompletableFuture<SqbResponse> precreate(PrecreateCommand command) {
        String clientSn = clientSnGenerator.generate();

        PrecreateRequest request = new PrecreateRequest();
        request.setTerminalSn(config.getTerminalSn());
        request.setClientSn(clientSn);
        request.setTotalAmount(String.valueOf(command.totalAmount()));
        request.setPayway(command.payway());
        request.setSubject(command.subject());
        request.setOperator(command.operator());
        request.setNotifyUrl(command.notifyUrl());

        SqbResponse sqbResponse = apiTemplate.call("/upay/v2/precreate", request);

        if (!sqbResponse.isCommunicationSuccess()) {
            log.error("预创建请求通信失败: {}", sqbResponse);
            return CompletableFuture.completedFuture(sqbResponse);
        }

        String bizResultCode = sqbResponse.getBizResultCode();

        if ("PRECREATE_FAIL".equals(bizResultCode)) {
            log.info("预创建失败: clientSn={}, reason={}", clientSn, sqbResponse.getBizErrorMessage());
            return CompletableFuture.completedFuture(sqbResponse);
        }

        if ("PRECREATE_SUCCESS".equals(bizResultCode)) {
            String orderStatus = sqbResponse.getOrderStatus();
            if (OrderStatus.isFinal(orderStatus) || !sqbResponse.getQrCode().isEmpty()) {
                log.info("预创建成功: clientSn={}, status={}", clientSn, orderStatus);
                return CompletableFuture.completedFuture(sqbResponse);
            }
        }

        log.info("预创建状态未确定，启动异步轮询查询: clientSn={}, bizResultCode={}", clientSn, bizResultCode);
        return queryService.pollByClientSn(clientSn);
    }
}
