package com.social.message.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.common.entity.Message;
import com.social.common.repository.MessageRepository;
import com.social.message.service.MessageServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
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
@RequiredArgsConstructor
public class MessageWebSocketHandler extends TextWebSocketHandler {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final MessageServiceImpl messageService;
    private final MessageRepository messageRepository;
    private final RedisMessageListenerContainer listenerContainer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();
    private final Map<String, Long> activeConversations = new ConcurrentHashMap<>();
    private final Map<Long, MessageListener> conversationListeners = new ConcurrentHashMap<>();

    private static final String MESSAGE_CHANNEL_PREFIX = "message:conversation:";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
            return;
        }
        userSessions.put(userId, session);
        sessionUsers.put(session.getId(), userId);
        log.info("Message WebSocket connected: userId={}", userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = objectMapper.readTree(message.getPayload());
        String type = json.path("type").asText();
        if ("ping".equals(type)) {
            session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            return;
        }
        if ("JOIN_CONVERSATION".equals(type)) {
            joinConversation(session, json.path("conversationId").asLong());
            return;
        }
        if ("LEAVE_CONVERSATION".equals(type)) {
            leaveConversation(session, json.path("conversationId").asLong());
        }
    }

    private void joinConversation(WebSocketSession session, Long conversationId) throws Exception {
        Long userId = sessionUsers.get(session.getId());
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
            return;
        }
        messageService.isConversationParticipant(conversationId, userId);
        activeConversations.put(session.getId(), conversationId);
        subscribeConversation(conversationId);
        session.sendMessage(new TextMessage("{\"type\":\"JOINED\",\"conversationId\":" + conversationId + "}"));
    }

    private void leaveConversation(WebSocketSession session, Long conversationId) {
        Long active = activeConversations.get(session.getId());
        if (active != null && active.equals(conversationId)) {
            activeConversations.remove(session.getId());
        }
    }

    private void subscribeConversation(Long conversationId) {
        conversationListeners.computeIfAbsent(conversationId, id -> {
            MessageListener listener = (redisMessage, pattern) -> handleRedisMessage(id, new String(redisMessage.getBody(), StandardCharsets.UTF_8));
            listenerContainer.addMessageListener(listener, new ChannelTopic(MESSAGE_CHANNEL_PREFIX + id));
            return listener;
        });
    }

    private void handleRedisMessage(Long conversationId, String body) {
        try {
            String[] parts = body.split(":");
            Long messageId = Long.parseLong(parts[0]);
            Message message = messageRepository.findById(messageId).orElse(null);
            if (message == null) {
                return;
            }
            WebSocketSession receiverSession = userSessions.get(message.getReceiverId());
            if (receiverSession == null || !receiverSession.isOpen()) {
                return;
            }
            Long activeConversationId = activeConversations.get(receiverSession.getId());
            if (!conversationId.equals(activeConversationId)) {
                return;
            }
            Map<String, Object> payload = Map.of(
                    "type", "MESSAGE",
                    "conversationId", conversationId,
                    "message", Map.of(
                            "id", message.getId(),
                            "conversationId", message.getConversationId(),
                            "senderId", message.getSenderId(),
                            "receiverId", message.getReceiverId(),
                            "messageType", message.getMessageType().name(),
                            "content", message.getContent() == null ? "" : message.getContent(),
                            "imageUrl", message.getImageUrl() == null ? "" : message.getImageUrl(),
                            "originalImageUrl", message.getOriginalImageUrl() == null ? "" : message.getOriginalImageUrl(),
                            "isRead", message.getIsRead(),
                            "createdAt", message.getCreatedAt() != null ? message.getCreatedAt().toString() : ""
                    )
            );
            receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception e) {
            log.error("Failed to push private message: conversationId={}, body={}, error={}", conversationId, body, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = sessionUsers.remove(session.getId());
        activeConversations.remove(session.getId());
        if (userId != null) {
            userSessions.remove(userId);
        }
        log.info("Message WebSocket closed: userId={}, status={}", userId, status);
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        String headerUserId = session.getHandshakeHeaders().getFirst("X-User-Id");
        if (headerUserId != null && !headerUserId.isEmpty()) {
            try {
                return Long.parseLong(headerUserId);
            } catch (NumberFormatException e) {
                log.warn("Invalid X-User-Id header: {}", headerUserId);
            }
        }

        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query == null) {
            return null;
        }
        String token = null;
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                token = param.substring(6);
                break;
            }
        }
        if (token == null) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            log.warn("Failed to parse message WebSocket token: {}", e.getMessage());
            return null;
        }
    }
}
