package com.social.gateway.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
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
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            log.warn("WebSocket connection rejected: invalid token");
            return;
        }

        userSessions.put(userId, session);
        log.info("WebSocket connected for user: {}", userId);

        // Subscribe to Redis notification channel for this user
        subscribeToNotifications(userId);

        // Send connection success message
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
            Map.of("type", "connected", "userId", userId)
        )));
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received message: {}", payload);

        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String type = (String) data.get("type");
            if ("ping".equals(type) || "ping".equals(payload)) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    Map.of("type", "pong")
                )));
            }
        } catch (Exception e) {
            if ("ping".equals(payload)) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    Map.of("type", "pong")
                )));
            }
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.remove(userId);
            unsubscribeFromNotifications(userId);
            log.info("WebSocket disconnected for user: {}", userId);
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }

    private Long getUserIdFromSession(WebSocketSession session) {
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
            return claims.get("userId", Long.class);
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
        String channel = "notification:channel:" + userId;

        MessageListener listener = (Message msg, byte[] pattern) -> {
            String body = new String(msg.getBody(), StandardCharsets.UTF_8);
            log.info("Received notification for user {}: {}", userId, body);

            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                try {
                    String[] parts = body.split(":");
                    if (parts.length >= 5) {
                        Map<String, Object> notification = Map.of(
                                "id", Long.parseLong(parts[0]),
                                "type", parts[1],
                                "actorId", Long.parseLong(parts[2]),
                                "targetId", Long.parseLong(parts[3]),
                                "targetType", parts[4]
                        );
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
            String channel = "notification:channel:" + userId;
            listenerContainer.removeMessageListener(listener, new ChannelTopic(channel));
            log.info("Unsubscribed from Redis channel: {}", channel);
        }
    }
}
