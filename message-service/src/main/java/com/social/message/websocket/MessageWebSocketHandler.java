package com.social.message.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.common.dto.MessageDTO;
import com.social.message.route.MessageRouteRegistry;
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

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageWebSocketHandler extends TextWebSocketHandler {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${server.port:8088}")
    private String serverPort;

    private final MessageServiceImpl messageService;
    private final RedisMessageListenerContainer listenerContainer;
    private final MessageRouteRegistry routeRegistry;
    private final ObjectMapper objectMapper;

    // 本实例唯一标识：IP:port:随机后缀，用于专属 channel 与路由表寻址
    private String instanceId;

    // 用户级 WebSocket 会话：userId -> session
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    // session -> userId 反向映射
    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();
    // session -> 当前活跃 conversationId（用户当前打开的会话）
    private final Map<String, Long> activeConversations = new ConcurrentHashMap<>();

    /**
     * 启动时生成 instanceId 并订阅本实例专属 channel。
     * 发送方通过路由表定位到本实例后，只往这个 channel 投递，不再全局广播。
     */
    @PostConstruct
    public void init() {
        this.instanceId = resolveInstanceId();
        String channel = MessageRouteRegistry.INSTANCE_CHANNEL_PREFIX + instanceId;
        listenerContainer.addMessageListener(
                (redisMessage, pattern) -> handleRedisMessage(new String(redisMessage.getBody(), StandardCharsets.UTF_8)),
                new ChannelTopic(channel)
        );
        log.info("Message WebSocket handler subscribed to instance channel: {}", channel);
    }

    private String resolveInstanceId() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            host = "unknown";
        }
        return host + ":" + serverPort + ":" + UUID.randomUUID().toString().substring(0, 8);
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
        // 登记路由：该用户连接落在本实例
        routeRegistry.register(userId, instanceId);
        log.info("Message WebSocket connected: userId={}, instanceId={}", userId, instanceId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = objectMapper.readTree(message.getPayload());
        String type = json.path("type").asText();
        if ("ping".equals(type)) {
            // 心跳兼顾路由续期
            Long userId = sessionUsers.get(session.getId());
            if (userId != null) {
                routeRegistry.refresh(userId, instanceId);
            }
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
        // 本地标记 + 路由表记录活跃会话
        activeConversations.put(session.getId(), conversationId);
        routeRegistry.updateActiveConversation(userId, instanceId, conversationId);
        session.sendMessage(new TextMessage("{\"type\":\"JOINED\",\"conversationId\":" + conversationId + "}"));
    }

    private void leaveConversation(WebSocketSession session, Long conversationId) {
        Long active = activeConversations.get(session.getId());
        if (active != null && active.equals(conversationId)) {
            activeConversations.remove(session.getId());
            Long userId = sessionUsers.get(session.getId());
            if (userId != null) {
                routeRegistry.updateActiveConversation(userId, instanceId, null);
            }
        }
    }

    /**
     * 处理投递到本实例专属 channel 的消息。因为是定向投递，receiver 必然在本机，
     * 只需再校验其当前活跃会话是否匹配（不匹配则不实时推送，走通知路径）。
     */
    private void handleRedisMessage(String body) {
        try {
            MessageDTO messageDTO = objectMapper.readValue(body, MessageDTO.class);
            WebSocketSession receiverSession = userSessions.get(messageDTO.getReceiverId());
            if (receiverSession == null || !receiverSession.isOpen()) {
                return;
            }
            Long activeConversationId = activeConversations.get(receiverSession.getId());
            if (!messageDTO.getConversationId().equals(activeConversationId)) {
                return;
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
            // 仅当路由仍指向本实例时才删除（避免误删已重连到别处的用户）
            routeRegistry.unregister(userId, instanceId);
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
