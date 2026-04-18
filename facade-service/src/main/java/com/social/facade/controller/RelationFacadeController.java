package com.social.facade.controller;

import com.social.common.api.NotificationService;
import com.social.common.api.PostService;
import com.social.common.api.RelationService;
import com.social.common.api.UserService;
import com.social.common.dto.PageResult;
import com.social.common.dto.RelationCountDTO;
import com.social.common.dto.Result;
import com.social.common.dto.UserDTO;
import com.social.common.dto.UserRelationDTO;
import com.social.common.enums.NotificationType;
import com.social.facade.dto.UserRelationFacadeResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/relations")
public class RelationFacadeController {

    @DubboReference(version = "1.0.0")
    private RelationService relationService;

    @DubboReference(version = "1.0.0")
    private UserService userService;

    @DubboReference(version = "1.0.0")
    private NotificationService notificationService;

    @DubboReference(version = "1.0.0")
    private PostService postService;

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
        List<UserRelationFacadeResponse> enriched = enrichRelationList(currentUserId, relations);
        return Result.success(enriched);
    }

    @GetMapping("/followers")
    public Result<List<UserRelationFacadeResponse>> getMyFollowersList(
            @RequestHeader("X-User-Id") Long currentUserId) {
        List<UserRelationDTO> relations = relationService.getFollowersList(currentUserId);
        List<UserRelationFacadeResponse> enriched = enrichRelationList(currentUserId, relations);
        return Result.success(enriched);
    }

    @GetMapping("/following/{userId}")
    public Result<PageResult<UserRelationFacadeResponse>> getFollowingList(
            @PathVariable(name = "userId") Long userId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        PageResult<UserRelationDTO> pagedRelations = relationService.getFollowingListPaged(userId, page, size);
        List<UserRelationFacadeResponse> enriched = enrichRelationListWithCounts(currentUserId, pagedRelations.getRecords());
        PageResult<UserRelationFacadeResponse> result = PageResult.of(
                enriched,
                pagedRelations.getTotal(),
                pagedRelations.getPage(),
                pagedRelations.getSize()
        );
        return Result.success(result);
    }

    @GetMapping("/followers/{userId}")
    public Result<PageResult<UserRelationFacadeResponse>> getFollowersList(
            @PathVariable(name = "userId") Long userId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        PageResult<UserRelationDTO> pagedRelations = relationService.getFollowersListPaged(userId, page, size);
        List<UserRelationFacadeResponse> enriched = enrichRelationListWithCounts(currentUserId, pagedRelations.getRecords());
        PageResult<UserRelationFacadeResponse> result = PageResult.of(
                enriched,
                pagedRelations.getTotal(),
                pagedRelations.getPage(),
                pagedRelations.getSize()
        );
        return Result.success(result);
    }

    @GetMapping("/counts/{userId}")
    public Result<RelationCountDTO> getRelationCounts(@PathVariable(name = "userId") Long userId) {
        RelationCountDTO counts = relationService.getRelationCounts(userId);
        Long postsCount = postService.getPostCount(userId);
        counts.setPostsCount(postsCount);
        return Result.success(counts);
    }

    @GetMapping("/is-following/{userId}")
    public Result<Boolean> isFollowing(
            @PathVariable(name = "userId") Long userId,
            @RequestHeader("X-User-Id") Long currentUserId) {
        return Result.success(relationService.isFollowing(currentUserId, userId));
    }

    private List<UserRelationFacadeResponse> enrichRelationList(Long currentUserId, List<UserRelationDTO> relations) {
        if (relations.isEmpty()) {
            return List.of();
        }

        // Batch query isFollowing for all users in the list
        List<Long> userIds = relations.stream().map(UserRelationDTO::getUserId).collect(Collectors.toList());
        Map<Long, Boolean> isFollowingMap = currentUserId != null
                ? relationService.areFollowing(currentUserId, userIds)
                : Map.of();

        return relations.stream()
                .map(relation -> {
                    String username = "未知用户";
                    String avatar = "";
                    RelationCountDTO counts = new RelationCountDTO(0L, 0L);
                    try {
                        UserDTO user = userService.getUserById(relation.getUserId());
                        username = user.getUsername() != null ? user.getUsername() : "未知用户";
                        avatar = user.getAvatar() != null ? user.getAvatar() : "";
                        counts = relationService.getRelationCounts(relation.getUserId());
                    } catch (Exception e) {
                        // Use defaults
                    }
                    return UserRelationFacadeResponse.of(
                            relation.getUserId(),
                            username,
                            avatar,
                            counts.getFollowingCount(),
                            counts.getFollowerCount(),
                            isFollowingMap.getOrDefault(relation.getUserId(), false)
                    );
                })
                .toList();
    }

    private List<UserRelationFacadeResponse> enrichRelationListWithCounts(Long currentUserId, List<UserRelationDTO> relations) {
        return enrichRelationList(currentUserId, relations);
    }
}
