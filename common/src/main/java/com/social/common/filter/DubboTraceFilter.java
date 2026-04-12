package com.social.common.filter;

import com.social.common.util.LogUtil;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

/**
 * Dubbo Filter for traceId propagation.
 * Consumer side: get traceId from MDC -> put into RpcContext
 * Provider side: get traceId from RpcContext -> put into MDC
 */
@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER}, order = -1000)
public class DubboTraceFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String traceId;
        boolean isProvider = RpcContext.getContext().isProviderSide();

        if (isProvider) {
            // Provider side: get traceId from RpcContext and set to MDC
            traceId = RpcContext.getContext().getAttachment(TRACE_ID_HEADER);
            if (traceId == null || traceId.isEmpty()) {
                traceId = LogUtil.generateTraceId();
            }
            LogUtil.setTraceId(traceId);
        } else {
            // Consumer side: get traceId from MDC and set to RpcContext
            traceId = LogUtil.getTraceId();
            if (traceId == null || traceId.isEmpty()) {
                traceId = LogUtil.generateTraceId();
            }
            RpcContext.getContext().setAttachment(TRACE_ID_HEADER, traceId);
        }

        try {
            return invoker.invoke(invocation);
        } finally {
            if (isProvider) {
                LogUtil.clearTraceId();
            }
        }
    }
}
