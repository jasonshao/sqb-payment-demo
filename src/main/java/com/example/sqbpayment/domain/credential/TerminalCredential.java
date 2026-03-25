package com.example.sqbpayment.domain.credential;

import java.time.LocalDateTime;

/**
 * 终端凭证（不可变快照）
 */
public record TerminalCredential(String terminalSn, String terminalKey, String deviceId, LocalDateTime updatedAt) {
}
