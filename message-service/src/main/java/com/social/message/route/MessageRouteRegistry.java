package com.social.message.route;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * 用户在线路由表（存于 Redis）。
 *
 * <p>解决"广播模型下 100 台实例都处理同一条消息"的问题：记录每个在线用户的
 * WebSocket 连接落在哪个 message-service 实例上，发消息时定向投递到目标实例的
 * 专属 channel，而非全局广播。</p>
 *
 * <p>结构：{@code route:user:{userId}} = Hash {@code { instanceId, activeConversation }}，TTL 90s。
 * 由 WebSocket 连接建立时写入、心跳续期、断开时删除。实例宕机时靠 TTL 自愈。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRouteRegistry {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String ROUTE_KEY_PREFIX = "route:user:";
    private static final String FIELD_INSTANCE = "instanceId";
    private static final String FIELD_ACTIVE_CONVERSATION = "activeConversation";
    private static final Duration ROUTE_TTL = Duration.ofSeconds(90);

    /** 专属 channel 前缀：每个实例只订阅 message:push:{instanceId}。 */
    public static final String INSTANCE_CHANNEL_PREFIX = "message:push:";

    private String routeKey(Long userId) {
        return ROUTE_KEY_PREFIX + userId;
    }

    /** 连接建立：登记用户所在实例，TTL 90s。 */
    public void register(Long userId, String instanceId) {
        String key = routeKey(userId);
        stringRedisTemplate.opsForHash().put(key, FIELD_INSTANCE, instanceId);
        stringRedisTemplate.expire(key, ROUTE_TTL);
    }

    /** 心跳续期：仅当路由仍指向本实例时才续，避免覆盖已迁移到别处的用户。 */
    public void refresh(Long userId, String instanceId) {
        String key = routeKey(userId);
        Object current = stringRedisTemplate.opsForHash().get(key, FIELD_INSTANCE);
        if (instanceId.equals(current)) {
            stringRedisTemplate.expire(key, ROUTE_TTL);
        } else {
            // 路由已不在本实例（迁移过），重新登记本实例
            register(userId, instanceId);
        }
    }

    /** 更新用户当前活跃会话（用于判断是否需要实时推送/是否发通知）。 */
    public void updateActiveConversation(Long userId, String instanceId, Long conversationId) {
        String key = routeKey(userId);
        if (conversationId == null) {
            stringRedisTemplate.opsForHash().delete(key, FIELD_ACTIVE_CONVERSATION);
        } else {
            stringRedisTemplate.opsForHash().put(key, FIELD_ACTIVE_CONVERSATION, String.valueOf(conversationId));
        }
        stringRedisTemplate.expire(key, ROUTE_TTL);
    }

    /** 断开连接：仅当路由仍指向本实例时才删除，避免误删已迁移的用户。 */
    public void unregister(Long userId, String instanceId) {
        String key = routeKey(userId);
        Object current = stringRedisTemplate.opsForHash().get(key, FIELD_INSTANCE);
        if (instanceId.equals(current)) {
            stringRedisTemplate.delete(key);
        }
    }

    /** 查询用户所在实例；不在线返回 null。 */
    public String getInstance(Long userId) {
        Object instance = stringRedisTemplate.opsForHash().get(routeKey(userId), FIELD_INSTANCE);
        return instance != null ? instance.toString() : null;
    }

    /** 查询用户当前活跃会话；无则返回 null。 */
    public Long getActiveConversation(Long userId) {
        Object conv = stringRedisTemplate.opsForHash().get(routeKey(userId), FIELD_ACTIVE_CONVERSATION);
        if (conv == null) {
            return null;
        }
        try {
            return Long.parseLong(conv.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 读取用户完整路由信息（instanceId + activeConversation），不在线返回 null。 */
    public RouteInfo getRoute(Long userId) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(routeKey(userId));
        if (entries.isEmpty()) {
            return null;
        }
        Object instance = entries.get(FIELD_INSTANCE);
        if (instance == null) {
            return null;
        }
        Long activeConversation = null;
        Object conv = entries.get(FIELD_ACTIVE_CONVERSATION);
        if (conv != null) {
            try {
                activeConversation = Long.parseLong(conv.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return new RouteInfo(instance.toString(), activeConversation);
    }

    public record RouteInfo(String instanceId, Long activeConversation) {
    }
}
