package com.example.sqbpayment.infrastructure.observability;

import java.util.regex.Pattern;

/**
 * 日志脱敏工具，防止敏感信息泄露到日志
 */
public final class LogSanitizer {

    private LogSanitizer() {}

    private static final Pattern TERMINAL_KEY_PATTERN =
        Pattern.compile("(terminal_key[\":]\\s*[\":]?)([^\"\\s,}]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DYNAMIC_ID_PATTERN =
        Pattern.compile("(dynamic_id[\":]\\s*[\":]?)([^\"\\s,}]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUTH_PATTERN =
        Pattern.compile("(Authorization[\":]\\s*[\":]?)([^\"\\s,}]{8})[^\"\\s,}]*", Pattern.CASE_INSENSITIVE);

    /**
     * Mask sensitive fields in the given text
     */
    public static String sanitize(String text) {
        if (text == null) return null;
        String result = TERMINAL_KEY_PATTERN.matcher(text).replaceAll("$1***");
        result = DYNAMIC_ID_PATTERN.matcher(result).replaceAll("$1***");
        result = AUTH_PATTERN.matcher(result).replaceAll("$1$2***");
        return result;
    }
}
