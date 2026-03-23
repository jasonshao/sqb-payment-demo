package com.example.sqbpayment.model.request;

/**
 * 撤单请求（Controller 入参）
 *
 * @param sn       收钱吧订单号（与 clientSn 二选一）
 * @param clientSn 商户订单号（与 sn 二选一）
 */
public record CancelCommand(
        String sn,
        String clientSn
) {
    /**
     * 校验：sn 和 clientSn 至少提供一个
     */
    public void validate() {
        if ((sn == null || sn.isBlank()) && (clientSn == null || clientSn.isBlank())) {
            throw new IllegalArgumentException("需要提供 sn 或 clientSn");
        }
    }
}
