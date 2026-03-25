package com.example.sqbpayment.controller;

import com.example.sqbpayment.model.ApiResult;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.response.TerminalResult;
import com.example.sqbpayment.service.SqbTerminalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 终端管理控制器：激活 + 签到
 */
@RestController
@RequestMapping("/api/terminal")
public class SqbTerminalController {

    private final SqbTerminalService terminalService;

    public SqbTerminalController(SqbTerminalService terminalService) {
        this.terminalService = terminalService;
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResult<TerminalResult>> activate(
            @RequestParam String code,
            @RequestParam String deviceId,
            @RequestParam(required = false) String name) {
        SqbResponse response = terminalService.activate(code, deviceId, name);
        return ResponseEntity.ok(new ApiResult<>(
                response.isCommunicationSuccess(), null, TerminalResult.from(response)));
    }

    @PostMapping("/checkin")
    public ResponseEntity<ApiResult<TerminalResult>> checkin() {
        SqbResponse response = terminalService.checkin();
        return ResponseEntity.ok(new ApiResult<>(
                response.isCommunicationSuccess(), null, TerminalResult.from(response)));
    }
}
