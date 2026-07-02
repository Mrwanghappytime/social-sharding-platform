package com.social.facade.dto;

import com.social.common.dto.NotificationDTO;
import lombok.Data;

import java.io.Serializable;

@Data
public class NotificationFacadeResponse implements Serializable {
    private Long id;
    private String type;
    private Long actorId;
    private String actorUsername;
    private String actorAvatar;
    private Long targetId;
    private String targetType;
    private String targetTitle;
    private ConversationFacadeResponse conversation;
    private Boolean isRead;
    private String createdAt;
    private String updatedAt;

    public static NotificationFacadeResponse fromNotificationDTO(NotificationDTO notification, String targetTitle) {
        NotificationFacadeResponse response = new NotificationFacadeResponse();
        response.setId(notification.getId());
        response.setType(notification.getType());
        response.setActorId(notification.getActorId());
        response.setActorUsername(notification.getActorUsername());
        response.setActorAvatar(notification.getActorAvatar());
        response.setTargetId(notification.getTargetId());
        response.setTargetType(notification.getTargetType());
        response.setTargetTitle(targetTitle);
        response.setIsRead(notification.getIsRead());
        response.setCreatedAt(notification.getCreatedAt());
        response.setUpdatedAt(notification.getUpdatedAt());
        return response;
    }
}
