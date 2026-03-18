package com.example.sqbpayment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 收钱吧配置类，从 application.yml 读取配置
 */
@Configuration
@ConfigurationProperties(prefix = "sqb")
public class SqbConfig {

    /** API 域名 */
    private String apiBase = "https://vsi-api.shouqianba.com";

    /** 服务商序列号（激活接口使用） */
    private String vendorSn;

    /** 服务商密钥（激活接口使用） */
    private String vendorKey;

    /** 应用 ID */
    private String appId;

    /** 终端序列号（激活后获得） */
    private String terminalSn;

    /** 终端密钥（激活/签到后获得，会动态更新） */
    private String terminalKey;

    /** 设备唯一标识 */
    private String deviceId;

    public String getApiBase() {
        return apiBase;
    }

    public void setApiBase(String apiBase) {
        this.apiBase = apiBase;
    }

    public String getVendorSn() {
        return vendorSn;
    }

    public void setVendorSn(String vendorSn) {
        this.vendorSn = vendorSn;
    }

    public String getVendorKey() {
        return vendorKey;
    }

    public void setVendorKey(String vendorKey) {
        this.vendorKey = vendorKey;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getTerminalSn() {
        return terminalSn;
    }

    public void setTerminalSn(String terminalSn) {
        this.terminalSn = terminalSn;
    }

    public String getTerminalKey() {
        return terminalKey;
    }

    public void setTerminalKey(String terminalKey) {
        this.terminalKey = terminalKey;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
