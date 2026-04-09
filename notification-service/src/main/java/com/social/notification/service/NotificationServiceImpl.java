package com.social.notification.service;

import com.social.common.api.NotificationService;
import com.social.common.api.UserService;
import com.social.common.constant.RedisKeys;
import com.social.common.dto.NotificationDTO;
import com.social.common.dto.PageResult;
import com.social.common.entity.Notification;
import com.social.common.enums.NotificationType;
import com.social.common.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@DubboService(interfaceClass = NotificationService.class, version = "1.0.0")
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @DubboReference(version = "1.0.0", check = false)
    private UserService userService;

    @Value("${notification.kol.follower-threshold:10000}")
    private Long kolFollowerThreshold;

    @Override
    public void sendNotification(Long recipientId, NotificationType type, Long actorId, Long targetId, String targetType) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setType(type);
        notification.setActorId(actorId);
        notification.setTargetId(targetId);
        notification.setTargetType(targetType);
        notification.setIsRead(false);

        Notification saved = notificationRepository.save(notification);

        log.info("Notification persisted: recipientId={}, type={}", recipientId, type);

        // Check if recipient is KOL - only non-KOL users receive real-time push
        boolean isKol = false;
        try {
            Long followerCount = userService.getFollowerCount(recipientId);
            isKol = followerCount != null && followerCount >= kolFollowerThreshold;
            log.info("Recipient {} follower count: {}, isKOL: {}", recipientId, followerCount, isKol);
        } catch (Exception e) {
            log.warn("Failed to get follower count for user {}, treating as non-KOL for real-time push: {}", recipientId, e.getMessage());
            isKol = false; // Default to non-KOL so they receive real-time notification
        }

        // For non-KOL users, send real-time notification via Redis Pub/Sub
        if (!isKol) {
            String channel = RedisKeys.notificationChannel(recipientId);
            // Enrich message with actor username and avatar
            String actorUsername = "unknown";
            String actorAvatar = "";
            try {
                var user = userService.getUserById(actorId);
                if (user != null) {
                    actorUsername = (user.getUsername() != null && !user.getUsername().isEmpty())
                        ? user.getUsername() : "unknown";
                    actorAvatar = (user.getAvatar() != null && !user.getAvatar().isEmpty())
                        ? user.getAvatar() : " ";
                }
            } catch (Exception e) {
                log.warn("Failed to get actor user info: actorId={}", actorId, e.getMessage());
            }
            // Format: id:type:actorId:targetId:targetType:actorUsername:actorAvatar
            String message = saved.getId() + ":" + type.name() + ":" + actorId + ":" + targetId + ":" + targetType + ":" + actorUsername + ":" + actorAvatar;
            stringRedisTemplate.convertAndSend(channel, message);
            log.info("Notification published to Redis channel: {}, message {}", channel, message);
        } else {
            log.info("Recipient {} is KOL, skipping Redis Pub/Sub (they will poll on login)", recipientId);
        }
    }

    @Override
    public PageResult<NotificationDTO> getNotificationList(Long recipientId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notificationPage = notificationRepository.findByRecipientId(recipientId, pageRequest);

        // Enrich with actor info
        List<NotificationDTO> dtoList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        for (Notification n : notificationPage.getContent()) {
            NotificationDTO dto = new NotificationDTO();
            dto.setId(n.getId());
            dto.setType(n.getType().name());
            dto.setActorId(n.getActorId());
            dto.setTargetId(n.getTargetId());
            dto.setTargetType(n.getTargetType());
            dto.setIsRead(n.getIsRead());
            dto.setCreatedAt(n.getCreatedAt() != null ? n.getCreatedAt().format(formatter) : null);

            // Enrich actor info
            try {
                var user = userService.getUserById(n.getActorId());
                if (user != null) {
                    dto.setActorUsername(user.getUsername());
                    dto.setActorAvatar(user.getAvatar() != null ? user.getAvatar() : "");
                } else {
                    dto.setActorUsername("未知用户");
                    dto.setActorAvatar("");
                }
            } catch (Exception e) {
                log.warn("Failed to get actor user info: actorId={}", n.getActorId(), e);
                dto.setActorUsername("未知用户");
                dto.setActorAvatar("");
            }

            dtoList.add(dto);
        }

        return PageResult.of(dtoList, notificationPage.getTotalElements(), page, size);
    }

    public void markAsRead(Long id, Long recipientId) {
        notificationRepository.findById(id).ifPresent(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
        log.info("Notification marked as read: id={}, recipientId={}", id, recipientId);
    }

    public void markAllAsRead(Long recipientId) {
        List<Notification> unreadNotifications = notificationRepository.findUnreadByRecipientId(recipientId);
        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
        log.info("All notifications marked as read: recipientId={}", recipientId);
    }

    public Long getUnreadCount(Long recipientId) {
        return notificationRepository.countUnreadByRecipientId(recipientId);
    }
}
