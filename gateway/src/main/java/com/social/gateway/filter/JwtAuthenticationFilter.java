package com.social.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_LOG_KEY = "traceId";

    @Value("${jwt.secret:social-sharing-platform-secret-key-change-in-production}")
    private String jwtSecret;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_NAME_HEADER = "X-User-Name";
    private static final String BEARER_PREFIX = "Bearer ";

    // Paths that don't require authentication
    private static final List<String> WHITE_LIST = List.of(
            "/api/users/login",
            "/api/users/register",
            "/actuator",
            "/api/posts/public",
            "/api/posts/feed",
            "/api/posts/search",
            "/api/posts/user/",
            "/api/files/",
            "/files/"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Generate and set traceId
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        final String finalTraceId = traceId;

        log.info("[{}] {} {}", finalTraceId, request.getMethod(), path);

        // Add traceId to request headers
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(TRACE_ID_HEADER, finalTraceId)
                .build();

        String authHeader = modifiedRequest.getHeaders().getFirst(AUTHORIZATION_HEADER);

        // Parse token if present (even on whitelisted routes for user info)
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            try {
                Claims claims = validateToken(token);
                Long userId = claims.get("userId", Long.class);
                String username = claims.getSubject();
                log.info("[{}] User authenticated: userId={}, username={}", finalTraceId, userId, username);

                // Add user info + traceId to request headers
                modifiedRequest = modifiedRequest.mutate()
                        .header(USER_ID_HEADER, String.valueOf(userId))
                        .header(USER_NAME_HEADER, username)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (Exception e) {
                // Token invalid/expired - for whitelisted paths, continue without user info
                // For non-whitelisted paths, this will be caught below
            }
        }

        // Reject non-whitelisted paths without valid token
        if (isWhiteListed(path)) {
            log.info("[{}] Path whitelisted, proceeding without auth", finalTraceId);
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        }

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange.getResponse(), "Missing or invalid Authorization header");
        }

        // This shouldn't be reached, but kept as safety net
        return unauthorized(exchange.getResponse(), "Invalid or expired token");
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private Claims validateToken(String token) {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");

        String body = String.format("{\"code\": 401, \"message\": \"%s\"}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100; // High priority
    }
}
