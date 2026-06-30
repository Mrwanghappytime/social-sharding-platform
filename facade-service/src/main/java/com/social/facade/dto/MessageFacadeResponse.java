package com.social.facade.dto;

import com.social.common.dto.MessageDTO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MessageFacadeResponse implements Serializable {
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

    public static MessageFacadeResponse fromMessageDTO(MessageDTO dto) {
        MessageFacadeResponse response = new MessageFacadeResponse();
        response.setId(dto.getId());
        response.setConversationId(dto.getConversationId());
        response.setSenderId(dto.getSenderId());
        response.setReceiverId(dto.getReceiverId());
        response.setMessageType(dto.getMessageType());
        response.setContent(dto.getContent());
        response.setImageUrl(dto.getImageUrl());
        response.setOriginalImageUrl(dto.getOriginalImageUrl());
        response.setIsRead(dto.getIsRead());
        response.setCreatedAt(dto.getCreatedAt());
        return response;
    }
}
