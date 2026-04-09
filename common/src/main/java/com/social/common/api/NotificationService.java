package com.social.common.api;

import com.social.common.dto.NotificationDTO;
import com.social.common.dto.PageResult;
import com.social.common.enums.NotificationType;

public interface NotificationService {

    void sendNotification(Long recipientId, NotificationType type, Long actorId, Long targetId, String targetType);

    PageResult<NotificationDTO> getNotificationList(Long recipientId, Integer page, Integer size);

    void markAsRead(Long id, Long recipientId);

    void markAllAsRead(Long recipientId);

    Long getUnreadCount(Long recipientId);
}
