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

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 预创建支付服务（C2B / 客扫商户码）
 *
 * 核心流程：
 * 1. 收银系统组装请求参数（含 payway）
 * 2. POST 到 /upay/v2/precreate
 * 3. 解析三层响应：通信层 -> 业务层 -> 订单状态
 * 4. 成功时返回 qr_code 供客户扫码
 * 5. 非最终状态时启动轮询查询
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

    /**
     * 发起预创建支付
     *
     * @param command 预创建命令（已通过 Bean Validation 校验）
     * @return 预创建结果（包含 qr_code 或经过轮询）
     */
    public CompletableFuture<SqbResponse> precreate(PrecreateCommand command) throws IOException, InterruptedException {
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

        // PRECREATE_IN_PROGRESS / 非最终状态 -> 异步轮询
        log.info("预创建状态未确定，启动异步轮询查询: clientSn={}, bizResultCode={}", clientSn, bizResultCode);
        return queryService.pollByClientSn(clientSn);
    }
}
