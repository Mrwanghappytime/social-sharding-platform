package com.social.facade.controller;

import com.social.common.api.InteractionService;
import com.social.common.api.NotificationService;
import com.social.common.api.PostService;
import com.social.common.api.UserService;
import com.social.common.dto.*;
import com.social.common.enums.NotificationType;
import com.social.facade.dto.CommentFacadeResponse;
import jakarta.validation.Valid;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/interactions")
public class InteractionFacadeController {

    @DubboReference(version = "1.0.0", check = false)
    private InteractionService interactionService;

    @DubboReference(version = "1.0.0", check = false)
    private PostService postService;

    @DubboReference(version = "1.0.0", check = false)
    private UserService userService;

    @DubboReference(version = "1.0.0", check = false)
    private NotificationService notificationService;

    @PostMapping("/posts/{postId}/like")
    public Result<Void> likePost(
            @PathVariable(name = "postId") Long postId,
            @RequestHeader("X-User-Id") Long userId) {
        // 1. 验证post存在
        if (!postService.isPostExists(postId)) {
            throw new com.social.common.exception.BusinessException(
                    com.social.common.exception.ErrorCode.POST_NOT_FOUND, "动态不存在");
        }

        // 2. 调用interactionService保存点赞
        interactionService.likePost(postId, userId);

        // 3. 增加点赞计数
        postService.incrementLikeCount(postId);

        // 4. 发送通知
        Long postOwnerId = postService.getUserIdByPostId(postId);
        if (!postOwnerId.equals(userId)) {
            UserDTO actor = userService.getUserById(userId);
            String actorUsername = actor.getUsername() != null ? actor.getUsername() : "未知用户";
            String actorAvatar = actor.getAvatar() != null ? actor.getAvatar() : "";
            notificationService.sendNotification(
                    postOwnerId, NotificationType.LIKE, userId, postId, "POST",
                    actorUsername, actorAvatar, false
            );
        }

        return Result.success();
    }

    @DeleteMapping("/posts/{postId}/like")
    public Result<Void> unlikePost(
            @PathVariable(name = "postId") Long postId,
            @RequestHeader("X-User-Id") Long userId) {
        // 1. 调用interactionService删除点赞
        interactionService.unlikePost(postId, userId);

        // 2. 减少点赞计数
        postService.decrementLikeCount(postId);

        return Result.success();
    }

    @GetMapping("/posts/{postId}/like")
    public Result<LikeStatusDTO> getLikeStatus(
            @PathVariable(name = "postId") Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        LikeStatusDTO status = interactionService.getLikeStatus(postId, userId);
        return Result.success(status);
    }

    @PostMapping("/posts/{postId}/comments")
    public Result<CommentFacadeResponse> commentOnPost(
            @PathVariable(name = "postId") Long postId,
            @Valid @RequestBody CreateCommentRequest createRequest,
            @RequestHeader("X-User-Id") Long userId) {
        // 1. 验证post存在
        if (!postService.isPostExists(postId)) {
            throw new com.social.common.exception.BusinessException(
                    com.social.common.exception.ErrorCode.POST_NOT_FOUND, "动态不存在");
        }

        // 2. 调用interactionService保存评论
        CommentDTO comment = interactionService.commentOnPost(postId, userId, createRequest.getContent());

        // 3. 增加评论计数
        postService.incrementCommentCount(postId);

        // 4. Enrich用户信息
        UserDTO user = userService.getUserById(userId);
        String username = user.getUsername() != null ? user.getUsername() : "未知用户";
        String userAvatar = user.getAvatar() != null ? user.getAvatar() : "";
        CommentFacadeResponse response = CommentFacadeResponse.fromCommentDTO(comment, username, userAvatar);

        // 5. 发送通知
        Long postOwnerId = postService.getUserIdByPostId(postId);
        if (!postOwnerId.equals(userId)) {
            notificationService.sendNotification(
                    postOwnerId, NotificationType.COMMENT, userId, postId, "POST",
                    username, userAvatar, false
            );
        }

        return Result.success(response);
    }

    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(
            @PathVariable(name = "id") Long id,
            @RequestHeader("X-User-Id") Long userId) {
        // 获取评论信息用于计数
        CommentDTO comment = interactionService.getComments(id, 1, 1).stream().findFirst().orElse(null);
        Long postId = null;
        if (comment != null) {
            postId = comment.getPostId();
        }

        // 1. 调用interactionService删除评论
        interactionService.deleteComment(id, userId);

        // 2. 减少评论计数
        if (postId != null) {
            postService.decrementCommentCount(postId);
        }

        return Result.success();
    }

    @GetMapping("/posts/{postId}/comments")
    public Result<List<CommentFacadeResponse>> getComments(
            @PathVariable(name = "postId") Long postId,
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size) {
        // 1. 获取评论列表
        List<CommentDTO> comments = interactionService.getComments(postId, page, size);

        // 2. Enrich用户信息
        List<CommentFacadeResponse> enrichedComments = comments.stream()
                .map(comment -> {
                    String username = "未知用户";
                    String userAvatar = "";
                    try {
                        UserDTO user = userService.getUserById(comment.getUserId());
                        username = user.getUsername() != null ? user.getUsername() : "未知用户";
                        userAvatar = user.getAvatar() != null ? user.getAvatar() : "";
                    } catch (Exception e) {
                        // Use defaults
                    }
                    return CommentFacadeResponse.fromCommentDTO(comment, username, userAvatar);
                })
                .toList();

        return Result.success(enrichedComments);
    }
}
