package com.social.common.util;

import org.slf4j.MDC;

public class LogUtil {

    public static final String TRACE_ID_KEY = "traceId";

    private LogUtil() {
    }

    /**
     * 获取当前线程的 traceId
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * 设置 traceId 到当前线程 MDC
     */
    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
    }

    /**
     * 清除 MDC 中的 traceId
     */
    public static void clearTraceId() {
        MDC.remove(TRACE_ID_KEY);
    }

    /**
     * 生成新的 traceId (16位十六进制)
     */
    public static String generateTraceId() {
        return Long.toHexString(System.currentTimeMillis()) + Long.toHexString(System.nanoTime());
    }
}
