package com.social.facade.filter;

import com.social.common.util.LogUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to extract traceId from HTTP Header and set into MDC.
 * Must run before other filters to ensure traceId is available for all logging.
 */
@Component
@Order(-1000)
public class TraceIdFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = LogUtil.generateTraceId();
        }

        try {
            LogUtil.setTraceId(traceId);
            // Also set as request attribute for potential use in controllers
            httpRequest.setAttribute("traceId", traceId);
            chain.doFilter(request, response);
        } finally {
            LogUtil.clearTraceId();
        }
    }
}
