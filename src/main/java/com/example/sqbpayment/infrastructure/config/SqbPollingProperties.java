package com.example.sqbpayment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sqb.polling")
public class SqbPollingProperties {
    private boolean enabled = true;
    private int timeoutMs = 120_000;
    private int fastIntervalMs = 3_000;
    private int slowIntervalMs = 10_000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public int getFastIntervalMs() { return fastIntervalMs; }
    public void setFastIntervalMs(int fastIntervalMs) { this.fastIntervalMs = fastIntervalMs; }
    public int getSlowIntervalMs() { return slowIntervalMs; }
    public void setSlowIntervalMs(int slowIntervalMs) { this.slowIntervalMs = slowIntervalMs; }
}
