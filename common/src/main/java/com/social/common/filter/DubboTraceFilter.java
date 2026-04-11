package com.social.common.filter;

import com.social.common.util.LogUtil;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Dubbo Filter for traceId propagation.
 * Consumer side: get traceId from MDC -> put into RpcContext
 * Provider side: get traceId from RpcContext -> put into MDC
 */
@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER}, order = -1000)
public class DubboTraceFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(DubboTraceFilter.class);
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String traceId;
        boolean isProvider = RpcContext.getContext().isProviderSide();

        log.info(">>> DubboTraceFilter ENTER | isProvider={} | method={} | invoker={}",
                isProvider, invocation.getMethodName(), invoker.getInterface().getSimpleName());

        if (isProvider) {
            // Provider side: get traceId from RpcContext and set to MDC
            traceId = RpcContext.getContext().getAttachment(TRACE_ID_HEADER);
            log.info("    Provider got attachment traceId={}", traceId);
            if (traceId == null || traceId.isEmpty()) {
                traceId = LogUtil.generateTraceId();
                log.info("    Provider generated new traceId={}", traceId);
            }
            LogUtil.setTraceId(traceId);
        } else {
            // Consumer side: get traceId from MDC and set to RpcContext
            traceId = LogUtil.getTraceId();
            log.info("    Consumer got MDC traceId={}", traceId);
            if (traceId == null || traceId.isEmpty()) {
                traceId = LogUtil.generateTraceId();
                log.info("    Consumer generated new traceId={}", traceId);
            }
            RpcContext.getContext().setAttachment(TRACE_ID_HEADER, traceId);
            log.info("    Consumer set attachment traceId={}", traceId);
        }

        try {
            Result result = invoker.invoke(invocation);
            log.info("<<< DubboTraceFilter EXIT | isProvider={} | traceId={} | result={}",
                    isProvider, LogUtil.getTraceId(), result != null ? "OK" : "NULL");
            return result;
        } catch (Exception e) {
            log.error("!!! DubboTraceFilter ERROR | isProvider={} | traceId={} | error={}",
                    isProvider, LogUtil.getTraceId(), e.getMessage());
            throw e;
        } finally {
            if (isProvider) {
                LogUtil.clearTraceId();
            }
        }
    }
}
