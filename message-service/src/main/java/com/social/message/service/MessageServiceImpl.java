package com.social.message.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.common.api.MessageService;
import com.social.common.dto.ConversationDTO;
import com.social.common.dto.MessageDTO;
import com.social.common.dto.PageResult;
import com.social.common.entity.Conversation;
import com.social.common.entity.Message;
import com.social.common.enums.MessageType;
import com.social.common.exception.BusinessException;
import com.social.common.exception.ErrorCode;
import com.social.common.repository.ConversationRepository;
import com.social.common.repository.MessageRepository;
import com.social.common.util.LogUtil;
import com.social.message.route.MessageRouteRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@DubboService(interfaceClass = MessageService.class, version = "1.0.0")
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final MessageRouteRegistry routeRegistry;

    /**
     * 消息推送不再全局广播，改为定向投递到 receiver 所在实例的专属 channel
     * （message:push:{instanceId}），由路由表 {@link MessageRouteRegistry} 定位实例。
     * 消除"N 台实例都处理同一条消息"的浪费。
     */

    @Override
    @Transactional
    public ConversationDTO getOrCreateConversation(Long currentUserId, Long targetUserId) {
        if (currentUserId == null || targetUserId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户不能为空");
        }
        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能给自己发私信");
        }

        long user1Id = Math.min(currentUserId, targetUserId);
        long user2Id = Math.max(currentUserId, targetUserId);

        return conversationRepository.findByUser1IdAndUser2Id(user1Id, user2Id)
                .map(conversation -> toConversationDTO(conversation, currentUserId))
                .orElseGet(() -> createConversation(currentUserId, user1Id, user2Id));
    }

    private ConversationDTO createConversation(Long currentUserId, Long user1Id, Long user2Id) {
        try {
            Conversation conversation = new Conversation();
            conversation.setUser1Id(user1Id);
            conversation.setUser2Id(user2Id);
            Conversation saved = conversationRepository.save(conversation);
            return toConversationDTO(saved, currentUserId);
        } catch (DataIntegrityViolationException e) {
            Conversation existing = conversationRepository.findByUser1IdAndUser2Id(user1Id, user2Id)
                    .orElseThrow(() -> e);
            return toConversationDTO(existing, currentUserId);
        }
    }

    @Override
    @Transactional
    public MessageDTO sendTextMessage(Long senderId, Long conversationId, String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息内容不能为空");
        }
        if (content.length() > 2000) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息内容不能超过2000字");
        }
        Conversation conversation = getConversationForParticipant(conversationId, senderId);
        Long receiverId = getPeerUserId(conversation, senderId);
        Message message = saveMessage(conversation, senderId, receiverId, MessageType.TEXT, content.trim(), null, null);
        updateConversationLastMessage(conversation, message, content.trim());
        MessageDTO dto = toMessageDTO(message);
        publishMessage(dto);
        return dto;
    }

    @Override
    @Transactional
    public MessageDTO sendImageMessage(Long senderId, Long conversationId, String imageUrl, String originalImageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片地址不能为空");
        }
        Conversation conversation = getConversationForParticipant(conversationId, senderId);
        Long receiverId = getPeerUserId(conversation, senderId);
        Message message = saveMessage(conversation, senderId, receiverId, MessageType.IMAGE, null, imageUrl, originalImageUrl);
        updateConversationLastMessage(conversation, message, "[图片]");
        MessageDTO dto = toMessageDTO(message);
        publishMessage(dto);
        return dto;
    }

    private Message saveMessage(Conversation conversation, Long senderId, Long receiverId, MessageType type,
                                String content, String imageUrl, String originalImageUrl) {
        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setMessageType(type);
        message.setContent(content);
        message.setImageUrl(imageUrl);
        message.setOriginalImageUrl(originalImageUrl);
        message.setIsRead(false);
        return messageRepository.save(message);
    }

    private void updateConversationLastMessage(Conversation conversation, Message message, String preview) {
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageType(message.getMessageType());
        conversation.setLastMessagePreview(truncatePreview(preview));
        conversation.setLastMessageAt(message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    private String truncatePreview(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 100 ? value.substring(0, 100) : value;
    }

    @Override
    public PageResult<MessageDTO> getMessages(Long conversationId, Long currentUserId, Integer page, Integer size) {
        getConversationForParticipant(conversationId, currentUserId);
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Message> messagePage = messageRepository.findByConversationId(conversationId, pageRequest);
        List<MessageDTO> records = messagePage.getContent().stream()
                .map(this::toMessageDTO)
                .toList();
        return PageResult.of(records, messagePage.getTotalElements(), page, size);
    }

    @Override
    public List<MessageDTO> getMessagesAfterId(Long conversationId, Long currentUserId, Long afterId) {
        getConversationForParticipant(conversationId, currentUserId);
        long safeAfterId = afterId != null ? afterId : 0L;
        return messageRepository.findByConversationIdAndIdGreaterThanOrderByIdAsc(conversationId, safeAfterId).stream()
                .map(this::toMessageDTO)
                .toList();
    }

    @Override
    public boolean isUserViewingConversation(Long userId, Long conversationId) {
        if (userId == null || conversationId == null) {
            return false;
        }
        // 路由表记录了用户在线所在实例及其当前活跃会话；匹配则说明对方正看着这个会话
        Long activeConversation = routeRegistry.getActiveConversation(userId);
        return conversationId.equals(activeConversation);
    }

    @Override
    public List<ConversationDTO> getConversationsByIds(List<Long> conversationIds, Long currentUserId) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Collections.emptyList();
        }
        return conversationRepository.findByIdsAndParticipant(conversationIds, currentUserId).stream()
                .map(conversation -> toConversationDTO(conversation, currentUserId))
                .toList();
    }

    @Override
    public ConversationDTO getConversationById(Long conversationId, Long currentUserId) {
        Conversation conversation = getConversationForParticipant(conversationId, currentUserId);
        return toConversationDTO(conversation, currentUserId);
    }

    @Override
    @Transactional
    public void markConversationAsRead(Long conversationId, Long currentUserId) {
        getConversationForParticipant(conversationId, currentUserId);
        int count = messageRepository.markConversationAsRead(conversationId, currentUserId);
        log.info("Conversation messages marked as read: conversationId={}, userId={}, count={}, traceId={}",
                conversationId, currentUserId, count, LogUtil.getTraceId());
    }

    @Override
    public Long getUnreadMessageCount(Long currentUserId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(currentUserId);
    }

    public boolean isConversationParticipant(Long conversationId, Long userId) {
        getConversationForParticipant(conversationId, userId);
        return true;
    }

    private Conversation getConversationForParticipant(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "会话不存在"));
        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }
        return conversation;
    }

    private Long getPeerUserId(Conversation conversation, Long currentUserId) {
        if (conversation.getUser1Id().equals(currentUserId)) {
            return conversation.getUser2Id();
        }
        if (conversation.getUser2Id().equals(currentUserId)) {
            return conversation.getUser1Id();
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该会话");
    }

    private ConversationDTO toConversationDTO(Conversation conversation, Long currentUserId) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());
        dto.setUser1Id(conversation.getUser1Id());
        dto.setUser2Id(conversation.getUser2Id());
        dto.setPeerUserId(getPeerUserId(conversation, currentUserId));
        dto.setLastMessageId(conversation.getLastMessageId());
        dto.setLastMessageType(conversation.getLastMessageType() != null ? conversation.getLastMessageType().name() : null);
        dto.setLastMessagePreview(conversation.getLastMessagePreview());
        dto.setLastMessageAt(conversation.getLastMessageAt());
        dto.setUnreadCount(messageRepository.countByConversationIdAndReceiverIdAndIsReadFalse(conversation.getId(), currentUserId));
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        return dto;
    }

    private MessageDTO toMessageDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversationId());
        dto.setSenderId(message.getSenderId());
        dto.setReceiverId(message.getReceiverId());
        dto.setMessageType(message.getMessageType().name());
        dto.setContent(message.getContent());
        dto.setImageUrl(message.getImageUrl());
        dto.setOriginalImageUrl(message.getOriginalImageUrl());
        dto.setIsRead(message.getIsRead());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }

    private void publishMessage(MessageDTO messageDTO) {
        try {
            // 定向投递：查路由表定位 receiver 所在实例，只发到该实例的专属 channel。
            // receiver 不在线（查不到路由）则不推送——消息已落库，靠对端上线/重连补拉恢复。
            String instanceId = routeRegistry.getInstance(messageDTO.getReceiverId());
            if (instanceId == null) {
                return;
            }
            String channel = MessageRouteRegistry.INSTANCE_CHANNEL_PREFIX + instanceId;
            String payload = objectMapper.writeValueAsString(messageDTO);
            stringRedisTemplate.convertAndSend(channel, payload);
        } catch (Exception e) {
            log.warn("Failed to publish message websocket event: messageId={}, error={}",
                    messageDTO.getId(), e.getMessage());
        }
    }
}
