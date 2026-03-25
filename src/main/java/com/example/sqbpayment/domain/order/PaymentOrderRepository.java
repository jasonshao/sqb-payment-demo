package com.example.sqbpayment.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByMerchantOrderNo(String merchantOrderNo);
    Optional<PaymentOrder> findByChannelOrderNo(String channelOrderNo);
}
