package com.social.common.entity;

import com.social.common.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    @Column(name = "actor_avatar", length = 500)
    private String actorAvatar;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "is_read")
    private Boolean isRead = false;
}
