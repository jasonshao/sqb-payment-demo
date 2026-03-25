package com.example.sqbpayment.infrastructure.config;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.sdk.responsegetter.LiveSqbResponseGetter;
import com.example.sqbpayment.sdk.responsegetter.SqbResponseGetter;
import com.example.sqbpayment.sdk.transport.RestClientSqbTransport;
import com.example.sqbpayment.sdk.transport.SqbTransport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SDK 基础设施 Bean 配置
 */
@Configuration
public class SqbSdkConfig {

    @Bean
    public SqbTransport sqbTransport() {
        return new RestClientSqbTransport();
    }

    @Bean
    public SqbResponseGetter sqbResponseGetter(SqbTransport transport, SqbConfig config) {
        return new LiveSqbResponseGetter(transport, config.getApiBase());
    }
}
