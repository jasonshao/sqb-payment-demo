package com.example.sqbpayment.domain.credential;

/**
 * 凭证提供者接口
 */
public interface SqbCredentialProvider {
    VendorCredential getVendorCredential();
    TerminalCredential getTerminalCredential(String deviceId);
    void updateTerminalCredential(String deviceId, TerminalCredential credential);
}
