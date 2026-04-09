package com.social.notification.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Component
@EnableWebSocket
public class NotificationWebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationHandler;

    public NotificationWebSocketConfig(NotificationWebSocketHandler notificationHandler) {
        this.notificationHandler = notificationHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationHandler, "/ws/notify")
                .setAllowedOrigins("*");
    }
}
