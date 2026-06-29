package com.social.common.api;

import com.social.common.dto.ConversationDTO;
import com.social.common.dto.MessageDTO;
import com.social.common.dto.PageResult;

import java.util.List;

public interface MessageService {

    ConversationDTO getOrCreateConversation(Long currentUserId, Long targetUserId);

    MessageDTO sendTextMessage(Long senderId, Long conversationId, String content);

    MessageDTO sendImageMessage(Long senderId, Long conversationId, String imageUrl, String originalImageUrl);

    PageResult<MessageDTO> getMessages(Long conversationId, Long currentUserId, Integer page, Integer size);

    List<ConversationDTO> getConversationsByIds(List<Long> conversationIds, Long currentUserId);

    ConversationDTO getConversationById(Long conversationId, Long currentUserId);

    void markConversationAsRead(Long conversationId, Long currentUserId);

    Long getUnreadMessageCount(Long currentUserId);
}
