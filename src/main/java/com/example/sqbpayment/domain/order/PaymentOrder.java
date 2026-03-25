package com.example.sqbpayment.domain.order;

import com.example.sqbpayment.model.enums.OrderStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_order")
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_order_no", nullable = false, length = 64)
    private String merchantOrderNo;

    @Column(name = "channel_order_no", length = 64)
    private String channelOrderNo;

    @Column(name = "refund_request_no", length = 64)
    private String refundRequestNo;

    @Column(name = "device_id", length = 128)
    private String deviceId;

    @Column(name = "order_type", nullable = false, length = 20)
    private String orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 30)
    private OrderStatus currentStatus;

    @Column(name = "amount")
    private Long amount;

    @Column(name = "last_channel_code", length = 64)
    private String lastChannelCode;

    @Column(name = "last_channel_message", length = 512)
    private String lastChannelMessage;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected PaymentOrder() {}

    public PaymentOrder(String merchantOrderNo, String orderType, OrderStatus currentStatus) {
        this.merchantOrderNo = merchantOrderNo;
        this.orderType = orderType;
        this.currentStatus = currentStatus;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // All getters and setters
    public Long getId() { return id; }
    public String getMerchantOrderNo() { return merchantOrderNo; }
    public void setMerchantOrderNo(String merchantOrderNo) { this.merchantOrderNo = merchantOrderNo; }
    public String getChannelOrderNo() { return channelOrderNo; }
    public void setChannelOrderNo(String channelOrderNo) { this.channelOrderNo = channelOrderNo; }
    public String getRefundRequestNo() { return refundRequestNo; }
    public void setRefundRequestNo(String refundRequestNo) { this.refundRequestNo = refundRequestNo; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getOrderType() { return orderType; }
    public OrderStatus getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(OrderStatus currentStatus) { this.currentStatus = currentStatus; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getLastChannelCode() { return lastChannelCode; }
    public void setLastChannelCode(String lastChannelCode) { this.lastChannelCode = lastChannelCode; }
    public String getLastChannelMessage() { return lastChannelMessage; }
    public void setLastChannelMessage(String lastChannelMessage) { this.lastChannelMessage = lastChannelMessage; }
    public int getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
