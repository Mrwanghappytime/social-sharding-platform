package com.social.facade.controller;

import com.social.common.api.NotificationService;
import com.social.common.api.RelationService;
import com.social.common.api.UserService;
import com.social.common.dto.RelationCountDTO;
import com.social.common.dto.Result;
import com.social.common.dto.UserDTO;
import com.social.common.dto.UserRelationDTO;
import com.social.common.enums.NotificationType;
import com.social.facade.dto.UserRelationFacadeResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/relations")
public class RelationFacadeController {

    @DubboReference(version = "1.0.0")
    private RelationService relationService;

    @DubboReference(version = "1.0.0")
    private UserService userService;

    @DubboReference(version = "1.0.0")
    private NotificationService notificationService;

    @PostMapping("/follow/{userId}")
    public Result<Void> follow(
            @PathVariable(name = "userId") Long userId,
            @RequestHeader("X-User-Id") Long currentUserId) {
        // 1. 验证目标用户存在
        if (!userService.isUserExists(userId)) {
            throw new com.social.common.exception.BusinessException(
                    com.social.common.exception.ErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        // 2. 调用relationService保存关注关系
        relationService.follow(currentUserId, userId);

        // 3. 发送通知
        UserDTO actor = userService.getUserById(currentUserId);
        String actorUsername = actor.getUsername() != null ? actor.getUsername() : "未知用户";
        String actorAvatar = actor.getAvatar() != null ? actor.getAvatar() : "";
        notificationService.sendNotification(
                userId, NotificationType.FOLLOW, currentUserId, userId, "USER",
                actorUsername, actorAvatar, false
        );

        return Result.success();
    }

    @DeleteMapping("/follow/{userId}")
    public Result<Void> unfollow(
            @PathVariable(name = "userId") Long userId,
            @RequestHeader("X-User-Id") Long currentUserId) {
        relationService.unfollow(currentUserId, userId);
        return Result.success();
    }

    @GetMapping("/following")
    public Result<List<UserRelationFacadeResponse>> getMyFollowingList(
            @RequestHeader("X-User-Id") Long currentUserId) {
        List<UserRelationDTO> relations = relationService.getFollowingList(currentUserId);
        List<UserRelationFacadeResponse> enriched = enrichRelationList(relations);
        return Result.success(enriched);
    }

    @GetMapping("/followers")
    public Result<List<UserRelationFacadeResponse>> getMyFollowersList(
            @RequestHeader("X-User-Id") Long currentUserId) {
        List<UserRelationDTO> relations = relationService.getFollowersList(currentUserId);
        List<UserRelationFacadeResponse> enriched = enrichRelationList(relations);
        return Result.success(enriched);
    }

    @GetMapping("/following/{userId}")
    public Result<List<UserRelationFacadeResponse>> getFollowingList(@PathVariable(name = "userId") Long userId) {
        List<UserRelationDTO> relations = relationService.getFollowingList(userId);
        List<UserRelationFacadeResponse> enriched = enrichRelationList(relations);
        return Result.success(enriched);
    }

    @GetMapping("/followers/{userId}")
    public Result<List<UserRelationFacadeResponse>> getFollowersList(@PathVariable(name = "userId") Long userId) {
        List<UserRelationDTO> relations = relationService.getFollowersList(userId);
        List<UserRelationFacadeResponse> enriched = enrichRelationList(relations);
        return Result.success(enriched);
    }

    @GetMapping("/counts/{userId}")
    public Result<RelationCountDTO> getRelationCounts(@PathVariable(name = "userId") Long userId) {
        return Result.success(relationService.getRelationCounts(userId));
    }

    @GetMapping("/is-following/{userId}")
    public Result<Boolean> isFollowing(
            @PathVariable(name = "userId") Long userId,
            @RequestHeader("X-User-Id") Long currentUserId) {
        return Result.success(relationService.isFollowing(currentUserId, userId));
    }

    private List<UserRelationFacadeResponse> enrichRelationList(List<UserRelationDTO> relations) {
        return relations.stream()
                .map(relation -> {
                    String username = "未知用户";
                    String avatar = "";
                    try {
                        UserDTO user = userService.getUserById(relation.getUserId());
                        username = user.getUsername() != null ? user.getUsername() : "未知用户";
                        avatar = user.getAvatar() != null ? user.getAvatar() : "";
                    } catch (Exception e) {
                        // Use defaults
                    }
                    return UserRelationFacadeResponse.of(relation.getUserId(), username, avatar);
                })
                .toList();
    }
}
