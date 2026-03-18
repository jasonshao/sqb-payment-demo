package com.example.sqbpayment.controller;

import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.service.SqbTerminalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

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

    /**
     * 终端激活
     * 激活码只能使用一次，激活成功后 terminal_sn 和 terminal_key 会被自动保存
     */
    @PostMapping("/activate")
    public ResponseEntity<Map<String, Object>> activate(
            @RequestParam String code,
            @RequestParam String deviceId,
            @RequestParam(required = false) String name) {
        try {
            SqbResponse response = terminalService.activate(code, deviceId, name);
            return buildResponse(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "激活请求失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 终端签到
     * 建议每天首次交易前调用，签到后 terminal_key 会更新
     */
    @PostMapping("/checkin")
    public ResponseEntity<Map<String, Object>> checkin() {
        try {
            SqbResponse response = terminalService.checkin();
            return buildResponse(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "签到请求失败: " + e.getMessage()
            ));
        }
    }

    private ResponseEntity<Map<String, Object>> buildResponse(SqbResponse response) {
        boolean success = response.isCommunicationSuccess();
        return ResponseEntity.ok(Map.of(
                "success", success,
                "resultCode", response.getResultCode(),
                "bizResultCode", response.getBizResultCode(),
                "terminalSn", response.getTerminalSn(),
                "terminalKey", response.getTerminalKey(),
                "raw", response.getRawResponse().toString()
        ));
    }
}
