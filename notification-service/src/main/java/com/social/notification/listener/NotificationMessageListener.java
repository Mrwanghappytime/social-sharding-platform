package com.social.notification.listener;

import com.social.notification.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMessageListener implements MessageListener {

    private final NotificationWebSocketHandler notificationWebSocketHandler;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());

        log.info("Received Redis message: channel={}, body={}", channel, body);

        // Extract userId from channel name: notification:channel:{userId}
        try {
            String[] parts = channel.split(":");
            if (parts.length == 3) {
                Long userId = Long.parseLong(parts[2]);
                notificationWebSocketHandler.sendNotificationToUser(userId, body);
            }
        } catch (NumberFormatException e) {
            log.error("Failed to parse userId from channel: {}", channel, e);
        }
    }
}
