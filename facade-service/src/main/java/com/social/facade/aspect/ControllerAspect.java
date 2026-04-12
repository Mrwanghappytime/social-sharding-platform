package com.social.facade.aspect;

import com.social.common.util.LogUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * AOP Aspect for logging controller method entry, exit and parameters.
 */
@Aspect
@Component
public class ControllerAspect {

    private static final Logger log = LoggerFactory.getLogger(ControllerAspect.class);

    @Pointcut("execution(* com.social.facade.controller..*.*(..))")
    public void controllerPointcut() {
    }

    @Before("controllerPointcut()")
    public void logMethodEntry(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String[] paramNames = signature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();

        StringBuilder sb = new StringBuilder();
        sb.append(">>> ").append(className).append(".").append(methodName).append(" ENTER");
        if (paramNames != null && paramNames.length > 0) {
            sb.append(" | params={");
            for (int i = 0; i < paramNames.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(paramNames[i]).append("=").append(safeToString(paramValues[i]));
            }
            sb.append("}");
        }
        log.info(sb.toString());
    }

    @Around("controllerPointcut()")
    public Object logMethodExit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String traceId = LogUtil.getTraceId();

        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            StringBuilder sb = new StringBuilder();
            sb.append("<<< ").append(className).append(".").append(methodName).append(" EXIT");
            sb.append(" | duration=").append(duration).append("ms");
            sb.append(" | traceId=").append(traceId);

            if (error != null) {
                sb.append(" | EXCEPTION=").append(error.getClass().getSimpleName());
                sb.append(":").append(error.getMessage());
                log.error(sb.toString(), error);
            } else {
                sb.append(" | result=").append(safeToString(result));
                log.info(sb.toString());
            }
        }
    }

    private String safeToString(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof byte[]) return "byte[" + ((byte[]) obj).length + "]";
        if (obj instanceof CharSequence) {
            String str = obj.toString();
            return str.length() > 200 ? str.substring(0, 200) + "..." : str;
        }
        if (obj.getClass().isArray()) {
            return "array[" + obj.getClass().getComponentType().getSimpleName() + "]";
        }
        String str = obj.toString();
        return str.length() > 200 ? str.substring(0, 200) + "..." : str;
    }
}
