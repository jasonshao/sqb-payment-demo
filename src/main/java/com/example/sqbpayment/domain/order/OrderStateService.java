package com.example.sqbpayment.domain.order;

import com.example.sqbpayment.model.enums.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OrderStateService {

    private static final Logger log = LoggerFactory.getLogger(OrderStateService.class);

    private final PaymentOrderRepository orderRepository;

    public OrderStateService(PaymentOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public PaymentOrder createOrder(String merchantOrderNo, String orderType, String deviceId, Long amount) {
        Optional<PaymentOrder> existing = orderRepository.findByMerchantOrderNo(merchantOrderNo);
        if (existing.isPresent()) {
            log.info("订单已存在: merchantOrderNo={}, status={}", merchantOrderNo, existing.get().getCurrentStatus());
            return existing.get();
        }

        PaymentOrder order = new PaymentOrder(merchantOrderNo, orderType, OrderStatus.CREATED);
        order.setDeviceId(deviceId);
        order.setAmount(amount);
        order = orderRepository.save(order);
        log.info("创建订单: merchantOrderNo={}, type={}", merchantOrderNo, orderType);
        return order;
    }

    @Transactional
    public boolean transitionState(String merchantOrderNo, OrderStatus newStatus,
                                   String channelCode, String channelMessage) {
        Optional<PaymentOrder> optOrder = orderRepository.findByMerchantOrderNo(merchantOrderNo);
        if (optOrder.isEmpty()) {
            log.warn("订单不存在，无法转换状态: merchantOrderNo={}", merchantOrderNo);
            return false;
        }

        PaymentOrder order = optOrder.get();
        OrderStatus currentStatus = order.getCurrentStatus();

        if (currentStatus.isFinalState()) {
            log.info("订单已在最终状态，忽略: merchantOrderNo={}, current={}, attempted={}",
                    merchantOrderNo, currentStatus, newStatus);
            return false;
        }

        order.setCurrentStatus(newStatus);
        order.setLastChannelCode(channelCode);
        order.setLastChannelMessage(channelMessage);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("订单状态转换: merchantOrderNo={}, {} -> {}", merchantOrderNo, currentStatus, newStatus);
        return true;
    }

    @Transactional
    public boolean transitionStateByChannelOrderNo(String channelOrderNo, OrderStatus newStatus,
                                                    String channelCode, String channelMessage) {
        Optional<PaymentOrder> optOrder = orderRepository.findByChannelOrderNo(channelOrderNo);
        if (optOrder.isEmpty()) {
            log.warn("订单不存在: channelOrderNo={}", channelOrderNo);
            return false;
        }

        PaymentOrder order = optOrder.get();
        return transitionState(order.getMerchantOrderNo(), newStatus, channelCode, channelMessage);
    }

    public Optional<PaymentOrder> findByMerchantOrderNo(String merchantOrderNo) {
        return orderRepository.findByMerchantOrderNo(merchantOrderNo);
    }

    public Optional<PaymentOrder> findByChannelOrderNo(String channelOrderNo) {
        return orderRepository.findByChannelOrderNo(channelOrderNo);
    }
}
