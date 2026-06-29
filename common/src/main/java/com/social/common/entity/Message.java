package com.social.common.entity;

import com.social.common.enums.MessageType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
        name = "messages",
        indexes = {
                @Index(name = "idx_conversation_created", columnList = "conversation_id, created_at"),
                @Index(name = "idx_receiver_read", columnList = "receiver_id, is_read"),
                @Index(name = "idx_sender_receiver_created", columnList = "sender_id, receiver_id, created_at")
        }
)
public class Message extends BaseEntity {

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    @Column(name = "content", length = 2000)
    private String content;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "original_image_url", length = 500)
    private String originalImageUrl;

    @Column(name = "is_read")
    private Boolean isRead = false;
}
