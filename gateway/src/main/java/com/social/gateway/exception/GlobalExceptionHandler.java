package com.social.gateway.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Order(-2) // Execute before default exception handlers
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        String path = exchange.getRequest().getPath().value();
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");

        // Log full stack trace for all errors
        log.error("[{}] {} {} - Error: {}", traceId, exchange.getRequest().getMethod(), path, ex.getMessage(), ex);

        // Set default status code
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Internal server error";

        if (ex instanceof ExpiredJwtException) {
            status = HttpStatus.UNAUTHORIZED;
            message = "Token has expired";
        } else if (ex instanceof MalformedJwtException) {
            status = HttpStatus.UNAUTHORIZED;
            message = "Malformed token";
        } else if (ex instanceof SignatureException) {
            status = HttpStatus.UNAUTHORIZED;
            message = "Invalid token signature";
        } else if (ex instanceof UnsupportedJwtException) {
            status = HttpStatus.UNAUTHORIZED;
            message = "Unsupported token format";
        } else if (ex instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) ex;
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : "Request error";
        } else if (ex.getMessage() != null && ex.getMessage().contains("401")) {
            status = HttpStatus.UNAUTHORIZED;
            message = "Authentication required";
        } else if (ex.getMessage() != null && ex.getMessage().contains("403")) {
            status = HttpStatus.FORBIDDEN;
            message = "Access denied";
        }

        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", status.value());
        errorResponse.put("message", message);
        errorResponse.put("path", path);
        errorResponse.put("traceId", traceId);
        errorResponse.put("timestamp", System.currentTimeMillis());
        if (ex.getMessage() != null) {
            errorResponse.put("errorDetail", ex.getMessage());
        }

        String body = toJsonString(errorResponse);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    private String toJsonString(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number) {
                sb.append(value);
            } else {
                sb.append("\"").append(value).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
