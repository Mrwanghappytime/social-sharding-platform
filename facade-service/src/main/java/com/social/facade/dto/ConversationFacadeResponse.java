package com.social.facade.dto;

import com.social.common.dto.ConversationDTO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ConversationFacadeResponse implements Serializable {
    private Long id;
    private Long peerUserId;
    private String peerUsername;
    private String peerAvatar;
    private Long lastMessageId;
    private String lastMessageType;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;

    public static ConversationFacadeResponse fromConversationDTO(ConversationDTO dto, String peerUsername, String peerAvatar) {
        ConversationFacadeResponse response = new ConversationFacadeResponse();
        response.setId(dto.getId());
        response.setPeerUserId(dto.getPeerUserId());
        response.setPeerUsername(peerUsername);
        response.setPeerAvatar(peerAvatar);
        response.setLastMessageId(dto.getLastMessageId());
        response.setLastMessageType(dto.getLastMessageType());
        response.setLastMessagePreview(dto.getLastMessagePreview());
        response.setLastMessageAt(dto.getLastMessageAt());
        response.setUnreadCount(dto.getUnreadCount());
        return response;
    }
}
