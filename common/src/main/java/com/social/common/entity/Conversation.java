package com.social.common.entity;

import com.social.common.enums.MessageType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
        name = "conversations",
        uniqueConstraints = @UniqueConstraint(name = "uk_conversation_users", columnNames = {"user1_id", "user2_id"}),
        indexes = {
                @Index(name = "idx_user1_updated", columnList = "user1_id, updated_at"),
                @Index(name = "idx_user2_updated", columnList = "user2_id, updated_at")
        }
)
public class Conversation extends BaseEntity {

    @Column(name = "user1_id", nullable = false)
    private Long user1Id;

    @Column(name = "user2_id", nullable = false)
    private Long user2Id;

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_message_type", length = 20)
    private MessageType lastMessageType;

    @Column(name = "last_message_preview", length = 255)
    private String lastMessagePreview;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;
}
