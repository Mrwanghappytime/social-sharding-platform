package com.social.notification.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.common.constant.RedisKeys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    @Value("${jwt.secret:social-sharing-platform-secret-key-change-in-production}")
    private String jwtSecret;

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Map from userId -> WebSocket session
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // Map from userId -> Redis message listener
    private final Map<Long, MessageListener> userListeners = new ConcurrentHashMap<>();

    public NotificationWebSocketHandler(StringRedisTemplate redisTemplate,
                                        RedisMessageListenerContainer listenerContainer) {
        this.redisTemplate = redisTemplate;
        this.listenerContainer = listenerContainer;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            log.warn("WebSocket connection rejected: invalid token");
            return;
        }

        userSessions.put(userId, session);
        log.info("WebSocket connected for user: {}", userId);

        // Subscribe to Redis notification channel for this user
        try {
            subscribeToNotifications(userId);
        } catch (Throwable t) {
            log.error("subscribeToNotifications failed for userId={} type={} msg={}",
                    userId, t.getClass().getName(), t.getMessage());
            for (StackTraceElement el : t.getStackTrace()) {
                log.error("    at {}.{}({}:{})",
                        el.getClassName(), el.getMethodName(), el.getFileName(), el.getLineNumber());
                if (el.getClassName().startsWith("com.social")) break;
            }
            throw t;
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received message: {}", payload);

        try {
            if ("ping".equals(payload)) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    Map.of("type", "pong")
                )));
            }
        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.remove(userId);
            unsubscribeFromNotifications(userId);
            log.info("WebSocket disconnected for user: {}", userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // 打印完整堆栈，方便排查 WebSocket 协议异常
        log.error("WebSocket transport error for session {}: type={} msg={}",
                session.getId(),
                exception.getClass().getName(),
                exception.getMessage());
        // 手动遍历 cause 链
        Throwable cause = exception;
        int depth = 0;
        while (cause != null && depth < 10) {
            log.error("  caused by[{}]: {} - {}", depth, cause.getClass().getName(), cause.getMessage());
            for (StackTraceElement el : cause.getStackTrace()) {
                if (el.getClassName().startsWith("com.social") ||
                    el.getClassName().startsWith("org.springframework.web.socket") ||
                    el.getClassName().startsWith("org.apache.tomcat.websocket")) {
                    log.error("    at {}.{}({}:{})", el.getClassName(), el.getMethodName(), el.getFileName(), el.getLineNumber());
                }
            }
            cause = cause.getCause();
            depth++;
        }
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }

    /**
     * Send notification to specific user via WebSocket
     * Message format: id:type:actorId:targetId:targetType:actorUsername:actorAvatar
     */
    public void sendNotificationToUser(Long userId, String message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String[] parts = message.split(":");
                if (parts.length >= 5) {
                    Map<String, Object> notification = new ConcurrentHashMap<>();
                    notification.put("id", Long.parseLong(parts[0]));
                    notification.put("type", parts[1]);
                    notification.put("actorId", Long.parseLong(parts[2]));
                    notification.put("targetId", Long.parseLong(parts[3]));
                    notification.put("targetType", parts[4]);
                    if (parts.length >= 7) {
                        notification.put("actorUsername", parts[5]);
                        notification.put("actorAvatar", parts[6]);
                    }
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(notification)));
                    log.info("Notification sent via WebSocket: userId={}, notification={}", userId, notification);
                } else {
                    // Fallback for old format
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                log.error("Failed to send WebSocket message: userId={}", userId, e);
            }
        } else {
            log.warn("WebSocket session not found or closed: userId={}", userId);
        }
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        // 1. 优先从 Gateway 注入的 X-User-Id header 读取（生产路径：通过 Gateway）
        String headerUserId = session.getHandshakeHeaders().getFirst("X-User-Id");
        if (headerUserId != null && !headerUserId.isEmpty()) {
            try {
                Long userId = Long.parseLong(headerUserId);
                log.debug("UserId resolved from X-User-Id header: {}", userId);
                return userId;
            } catch (NumberFormatException e) {
                log.warn("Invalid X-User-Id header value: {}", headerUserId);
            }
        }

        // 2. fallback: 从 query string 解析 token（直连场景，便于本地调试）
        String token = extractToken(session);
        if (token == null) {
            return null;
        }

        try {
            byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(keyBytes))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = claims.get("userId", Long.class);
            log.debug("UserId resolved from query token: {}", userId);
            return userId;
        } catch (Exception e) {
            log.warn("Failed to parse JWT from WebSocket session: {}", e.getMessage());
            return null;
        }
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null && query.contains("token=")) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }
        return null;
    }

    private void subscribeToNotifications(Long userId) {
        String channel = RedisKeys.notificationChannel(userId);

        MessageListener listener = (Message msg, byte[] pattern) -> {
            String body = new String(msg.getBody(), StandardCharsets.UTF_8);
            log.info("Received notification for user {}: {}", userId, body);

            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                try {
                    String[] parts = body.split(":");
                    if (parts.length >= 5) {
                        Map<String, Object> notification = new ConcurrentHashMap<>();
                        notification.put("id", Long.parseLong(parts[0]));
                        notification.put("type", parts[1]);
                        notification.put("actorId", Long.parseLong(parts[2]));
                        notification.put("targetId", Long.parseLong(parts[3]));
                        notification.put("targetType", parts[4]);
                        if (parts.length >= 7) {
                            notification.put("actorUsername", parts[5]);
                            notification.put("actorAvatar", parts[6]);
                        }
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(notification)));
                    }
                } catch (Exception e) {
                    log.error("Failed to send WebSocket message to user {}: {}", userId, e.getMessage());
                }
            }
        };

        userListeners.put(userId, listener);
        listenerContainer.addMessageListener(listener, new ChannelTopic(channel));
        log.info("Subscribed to Redis channel: {}", channel);
    }

    private void unsubscribeFromNotifications(Long userId) {
        MessageListener listener = userListeners.remove(userId);
        if (listener != null) {
            String channel = RedisKeys.notificationChannel(userId);
            listenerContainer.removeMessageListener(listener, new ChannelTopic(channel));
            log.info("Unsubscribed from Redis channel: {}", channel);
        }
    }
}
