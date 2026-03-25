package com.example.sqbpayment.sdk.exception;

/**
 * 收钱吧 API 层异常（通信层 result_code != 200）
 */
public class SqbApiException extends SqbException {

    private final String channelResultCode;
    private final String channelErrorCode;
    private final String rawResponseSummary;

    public SqbApiException(String message, String requestId, Integer httpStatus,
                           String channelResultCode, String channelErrorCode,
                           String rawResponseSummary) {
        super(message, requestId, httpStatus, false, message, null);
        this.channelResultCode = channelResultCode;
        this.channelErrorCode = channelErrorCode;
        this.rawResponseSummary = rawResponseSummary;
    }

    public String getChannelResultCode() {
        return channelResultCode;
    }

    public String getChannelErrorCode() {
        return channelErrorCode;
    }

    public String getRawResponseSummary() {
        return rawResponseSummary;
    }
}
