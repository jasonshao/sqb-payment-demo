package com.example.sqbpayment.domain.credential;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.credential.TerminalCredentialEntity;
import com.example.sqbpayment.credential.TerminalCredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DatabaseCredentialProvider implements SqbCredentialProvider {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCredentialProvider.class);

    private final SqbConfig config;
    private final TerminalCredentialRepository credentialRepository;
    private final ConcurrentHashMap<String, TerminalCredential> cache = new ConcurrentHashMap<>();

    public DatabaseCredentialProvider(SqbConfig config, TerminalCredentialRepository credentialRepository) {
        this.config = config;
        this.credentialRepository = credentialRepository;
    }

    @Override
    public VendorCredential getVendorCredential() {
        return new VendorCredential(config.getVendorSn(), config.getVendorKey());
    }

    @Override
    public TerminalCredential getTerminalCredential(String deviceId) {
        return cache.computeIfAbsent(deviceId, this::loadFromDb);
    }

    @Override
    public void updateTerminalCredential(String deviceId, TerminalCredential credential) {
        // Write-through: update cache and DB
        cache.put(deviceId, credential);

        TerminalCredentialEntity entity = new TerminalCredentialEntity();
        entity.setDeviceId(deviceId);
        entity.setTerminalSn(credential.terminalSn());
        entity.setTerminalKey(credential.terminalKey());
        entity.setUpdateTime(credential.updatedAt() != null ? credential.updatedAt() : LocalDateTime.now());
        credentialRepository.save(entity);

        // Also update SqbConfig for backward compatibility during transition
        config.setTerminalSn(credential.terminalSn());
        config.setTerminalKey(credential.terminalKey());

        log.info("终端凭证已更新: device_id={}, terminal_sn={}", deviceId, credential.terminalSn());
    }

    private TerminalCredential loadFromDb(String deviceId) {
        return credentialRepository.findById(deviceId)
                .map(entity -> new TerminalCredential(
                        entity.getTerminalSn(),
                        entity.getTerminalKey(),
                        entity.getDeviceId(),
                        entity.getUpdateTime()))
                .orElse(null);
    }
}
