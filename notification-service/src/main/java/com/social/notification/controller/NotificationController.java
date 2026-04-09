package com.social.notification.controller;

import com.social.common.dto.NotificationDTO;
import com.social.common.dto.PageResult;
import com.social.common.dto.Result;
import com.social.notification.service.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationServiceImpl notificationService;

    /**
     * Get notification list with pagination
     */
    @GetMapping
    public Result<PageResult<NotificationDTO>> getNotificationList(
            @RequestParam(name = "recipientId") Long recipientId,
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size) {
        log.info("Getting notification list: recipientId={}, page={}, size={}", recipientId, page, size);
        PageResult<NotificationDTO> result = notificationService.getNotificationList(recipientId, page, size);
        return Result.success(result);
    }

    /**
     * Mark single notification as read
     */
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable(name = "id") Long id, @RequestParam(name = "recipientId") Long recipientId) {
        log.info("Marking notification as read: id={}, recipientId={}", id, recipientId);
        notificationService.markAsRead(id, recipientId);
        return Result.success();
    }

    /**
     * Mark all notifications as read
     */
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(@RequestParam(name = "recipientId") Long recipientId) {
        log.info("Marking all notifications as read: recipientId={}", recipientId);
        notificationService.markAllAsRead(recipientId);
        return Result.success();
    }

    /**
     * Get unread notification count
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestParam(name = "recipientId") Long recipientId) {
        log.info("Getting unread notification count: recipientId={}", recipientId);
        Long count = notificationService.getUnreadCount(recipientId);
        return Result.success(count);
    }
}
