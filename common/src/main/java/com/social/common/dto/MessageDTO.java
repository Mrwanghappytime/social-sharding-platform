package com.social.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MessageDTO implements Serializable {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private String messageType;
    private String content;
    private String imageUrl;
    private String originalImageUrl;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
