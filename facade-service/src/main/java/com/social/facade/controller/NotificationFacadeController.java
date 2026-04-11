package com.social.facade.controller;

import com.social.common.api.NotificationService;
import com.social.common.dto.NotificationDTO;
import com.social.common.dto.PageResult;
import com.social.common.dto.Result;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationFacadeController {

    @DubboReference(version = "1.0.0")
    private NotificationService notificationService;

    @GetMapping
    public Result<PageResult<NotificationDTO>> getNotificationList(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size) {
        return Result.success(notificationService.getNotificationList(userId, page, size));
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
}
