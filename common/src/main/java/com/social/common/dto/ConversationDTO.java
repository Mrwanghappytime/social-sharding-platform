package com.social.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ConversationDTO implements Serializable {
    private Long id;
    private Long user1Id;
    private Long user2Id;
    private Long peerUserId;
    private Long lastMessageId;
    private String lastMessageType;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
