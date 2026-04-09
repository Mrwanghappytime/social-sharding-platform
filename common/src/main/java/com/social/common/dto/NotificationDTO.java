package com.social.common.dto;

import lombok.Data;

@Data
public class NotificationDTO {
    private Long id;
    private String type;
    private Long actorId;
    private String actorUsername;
    private String actorAvatar;
    private Long targetId;
    private String targetType;
    private Boolean isRead;
    private String createdAt;
}
