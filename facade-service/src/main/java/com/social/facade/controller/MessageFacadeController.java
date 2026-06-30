package com.social.facade.controller;

import com.social.common.api.MessageService;
import com.social.common.api.NotificationService;
import com.social.common.api.UserService;
import com.social.common.dto.*;
import com.social.common.exception.BusinessException;
import com.social.common.exception.ErrorCode;
import com.social.facade.dto.ConversationFacadeResponse;
import com.social.facade.dto.MessageFacadeResponse;
import jakarta.validation.Valid;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
public class MessageFacadeController {

    @DubboReference(version = "1.0.0", check = false)
    private MessageService messageService;

    @DubboReference(version = "1.0.0", check = false)
    private UserService userService;

    @DubboReference(version = "1.0.0", check = false)
    private NotificationService notificationService;

    @PostMapping("/conversations")
    public Result<ConversationFacadeResponse> getOrCreateConversation(
            @RequestHeader("X-User-Id") Long currentUserId,
            @Valid @RequestBody CreateConversationRequest request) {
        if (currentUserId.equals(request.getTargetUserId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能给自己发私信");
        }
        if (!userService.isUserExists(request.getTargetUserId())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        ConversationDTO conversation = messageService.getOrCreateConversation(currentUserId, request.getTargetUserId());
        return Result.success(enrichConversation(conversation));
    }

    @GetMapping("/conversations/{conversationId}")
    public Result<ConversationFacadeResponse> getConversation(
            @RequestHeader("X-User-Id") Long currentUserId,
            @PathVariable Long conversationId) {
        ConversationDTO conversation = messageService.getConversationById(conversationId, currentUserId);
        return Result.success(enrichConversation(conversation));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public Result<PageResult<MessageFacadeResponse>> getMessages(
            @RequestHeader("X-User-Id") Long currentUserId,
            @PathVariable Long conversationId,
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "30") Integer size) {
        PageResult<MessageDTO> messages = messageService.getMessages(conversationId, currentUserId, page, size);
        List<MessageFacadeResponse> records = messages.getRecords().stream()
                .map(MessageFacadeResponse::fromMessageDTO)
                .toList();
        return Result.success(PageResult.of(records, messages.getTotal(), messages.getPage(), messages.getSize()));
    }

    @PostMapping("/conversations/{conversationId}/messages/text")
    public Result<MessageFacadeResponse> sendTextMessage(
            @RequestHeader("X-User-Id") Long currentUserId,
            @PathVariable Long conversationId,
            @Valid @RequestBody SendTextMessageRequest request) {
        MessageDTO message = messageService.sendTextMessage(currentUserId, conversationId, request.getContent());
        upsertMessageNotification(currentUserId, message.getReceiverId(), conversationId);
        return Result.success(MessageFacadeResponse.fromMessageDTO(message));
    }

    @PostMapping("/conversations/{conversationId}/messages/image")
    public Result<MessageFacadeResponse> sendImageMessage(
            @RequestHeader("X-User-Id") Long currentUserId,
            @PathVariable Long conversationId,
            @Valid @RequestBody SendImageMessageRequest request) {
        MessageDTO message = messageService.sendImageMessage(currentUserId, conversationId, request.getImageUrl(), request.getOriginalImageUrl());
        upsertMessageNotification(currentUserId, message.getReceiverId(), conversationId);
        return Result.success(MessageFacadeResponse.fromMessageDTO(message));
    }

    @PutMapping("/conversations/{conversationId}/read")
    public Result<Void> markConversationAsRead(
            @RequestHeader("X-User-Id") Long currentUserId,
            @PathVariable Long conversationId) {
        messageService.markConversationAsRead(conversationId, currentUserId);
        notificationService.markConversationNotificationAsRead(currentUserId, conversationId);
        return Result.success();
    }

    private void upsertMessageNotification(Long senderId, Long receiverId, Long conversationId) {
        UserDTO sender = userService.getUserById(senderId);
        String username = sender.getUsername() != null ? sender.getUsername() : "未知用户";
        String avatar = sender.getAvatar() != null ? sender.getAvatar() : "";
        notificationService.upsertConversationNotification(receiverId, senderId, conversationId, username, avatar);
    }

    private ConversationFacadeResponse enrichConversation(ConversationDTO conversation) {
        UserDTO peer = userService.getUserById(conversation.getPeerUserId());
        String username = peer.getUsername() != null ? peer.getUsername() : "未知用户";
        String avatar = peer.getAvatar() != null ? peer.getAvatar() : "";
        return ConversationFacadeResponse.fromConversationDTO(conversation, username, avatar);
    }
}
