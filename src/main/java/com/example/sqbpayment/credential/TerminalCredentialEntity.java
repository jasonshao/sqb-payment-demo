package com.example.sqbpayment.credential;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 终端凭证持久化实体
 * 存储 terminal_sn 和 terminal_key，防止重启丢失
 */
@Entity
@Table(name = "terminal_credential")
public class TerminalCredentialEntity {

    @Id
    @Column(name = "device_id", length = 128)
    private String deviceId;

    @Column(name = "terminal_sn", length = 128)
    private String terminalSn;

    @Column(name = "terminal_key", length = 128)
    private String terminalKey;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
