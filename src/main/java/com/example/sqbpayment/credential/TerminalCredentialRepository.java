package com.example.sqbpayment.credential;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 终端凭证 Repository
 */
@Repository
public interface TerminalCredentialRepository extends JpaRepository<TerminalCredentialEntity, String> {
}
