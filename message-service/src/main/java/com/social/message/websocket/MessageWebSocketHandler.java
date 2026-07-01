package com.social.message.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.common.dto.MessageDTO;
import com.social.message.service.MessageServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final RedisMessageListenerContainer listenerContainer;
    private final ObjectMapper objectMapper;

    // 用户级 WebSocket 会话：userId -> session
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    // session -> userId 反向映射
    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();
    // session -> 当前活跃 conversationId（用户当前打开的会话）
    private final Map<String, Long> activeConversations = new ConcurrentHashMap<>();

    /**
     * 启动时订阅全局 channel。所有实例都收到广播，各自判断本机是否有目标用户。
     * 不再按 conversation 动态订阅/取消订阅。
     */
    @PostConstruct
    public void init() {
        listenerContainer.addMessageListener(
                (redisMessage, pattern) -> handleRedisMessage(new String(redisMessage.getBody(), StandardCharsets.UTF_8)),
                new ChannelTopic(MessageServiceImpl.MESSAGE_PUSH_CHANNEL)
        );
        log.info("Message WebSocket handler subscribed to global channel: {}",
                MessageServiceImpl.MESSAGE_PUSH_CHANNEL);
    }

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
        // 权限校验
        messageService.isConversationParticipant(conversationId, userId);
        // 只标记当前活跃会话，不做 Redis 订阅
        activeConversations.put(session.getId(), conversationId);
        session.sendMessage(new TextMessage("{\"type\":\"JOINED\",\"conversationId\":" + conversationId + "}"));
    }

    private void leaveConversation(WebSocketSession session, Long conversationId) {
        Long active = activeConversations.get(session.getId());
        if (active != null && active.equals(conversationId)) {
            activeConversations.remove(session.getId());
        }
    }

    /**
     * 处理来自全局 Redis channel 的消息。
     * 每个 message-service 实例都会收到，但只有 receiver 所在的实例才实际推送。
     */
    private void handleRedisMessage(String body) {
        try {
            MessageDTO messageDTO = objectMapper.readValue(body, MessageDTO.class);
            Long receiverId = messageDTO.getReceiverId();
            // 判断 receiver 是否在本机
            WebSocketSession receiverSession = userSessions.get(receiverId);
            if (receiverSession == null || !receiverSession.isOpen()) {
                return; // 用户不在本机，忽略
            }
            // 判断 receiver 当前活跃会话是否是这条消息所属会话
            Long activeConversationId = activeConversations.get(receiverSession.getId());
            if (!messageDTO.getConversationId().equals(activeConversationId)) {
                return; // 用户没打开这个会话，走通知路径而非实时推送
            }
            Map<String, Object> payload = Map.of(
                    "type", "MESSAGE",
                    "conversationId", messageDTO.getConversationId(),
                    "message", Map.of(
                            "id", messageDTO.getId(),
                            "conversationId", messageDTO.getConversationId(),
                            "senderId", messageDTO.getSenderId(),
                            "receiverId", messageDTO.getReceiverId(),
                            "messageType", messageDTO.getMessageType(),
                            "content", messageDTO.getContent() == null ? "" : messageDTO.getContent(),
                            "imageUrl", messageDTO.getImageUrl() == null ? "" : messageDTO.getImageUrl(),
                            "originalImageUrl", messageDTO.getOriginalImageUrl() == null ? "" : messageDTO.getOriginalImageUrl(),
                            "isRead", messageDTO.getIsRead(),
                            "createdAt", messageDTO.getCreatedAt() != null ? messageDTO.getCreatedAt().toString() : ""
                    )
            );
            receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception e) {
            log.error("Failed to push private message: body={}, error={}", body, e.getMessage());
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
