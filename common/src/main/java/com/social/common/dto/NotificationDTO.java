package com.social.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO implements Serializable {
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
