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

    List<MessageDTO> getMessagesAfterId(Long conversationId, Long currentUserId, Long afterId);

    /**
     * 判断用户当前是否正在查看指定会话（WebSocket 活跃会话匹配）。
     * facade 发送私聊通知前调用：若对方正在该会话中，则消息已实时推送，无需再发通知。
     */
    boolean isUserViewingConversation(Long userId, Long conversationId);

    List<ConversationDTO> getConversationsByIds(List<Long> conversationIds, Long currentUserId);

    ConversationDTO getConversationById(Long conversationId, Long currentUserId);

    void markConversationAsRead(Long conversationId, Long currentUserId);

    Long getUnreadMessageCount(Long currentUserId);
}
