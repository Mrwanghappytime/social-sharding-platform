package com.social.facade.controller;

import com.social.common.api.NotificationService;
import com.social.common.api.PostService;
import com.social.common.dto.NotificationDTO;
import com.social.common.dto.PageResult;
import com.social.common.dto.PostDTO;
import com.social.common.dto.Result;
import com.social.facade.dto.NotificationFacadeResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notifications")
public class NotificationFacadeController {

    @DubboReference(version = "1.0.0")
    private NotificationService notificationService;

    @DubboReference(version = "1.0.0")
    private PostService postService;

    @GetMapping
    public Result<PageResult<NotificationFacadeResponse>> getNotificationList(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size) {
        PageResult<NotificationDTO> notifications = notificationService.getNotificationList(userId, page, size);
        Map<Long, String> postTitleMap = getPostTitleMap(notifications.getRecords());

        List<NotificationFacadeResponse> records = notifications.getRecords().stream()
                .map(notification -> NotificationFacadeResponse.fromNotificationDTO(
                        notification,
                        postTitleMap.get(notification.getTargetId())
                ))
                .toList();

        return Result.success(PageResult.of(records, notifications.getTotal(), notifications.getPage(), notifications.getSize()));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(
            @PathVariable(name = "id") Long id,
            @RequestHeader("X-User-Id") Long userId) {
        notificationService.markAsRead(id, userId);
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(@RequestHeader("X-User-Id") Long userId) {
        notificationService.markAllAsRead(userId);
        return Result.success();
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(notificationService.getUnreadCount(userId));
    }

    private Map<Long, String> getPostTitleMap(List<NotificationDTO> notifications) {
        List<Long> postIds = notifications.stream()
                .filter(notification -> "POST".equals(notification.getTargetType()))
                .map(NotificationDTO::getTargetId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return postService.getPostsByIds(postIds).stream()
                .filter(post -> post.getId() != null)
                .collect(Collectors.toMap(PostDTO::getId, PostDTO::getTitle, (left, right) -> left));
    }
}
