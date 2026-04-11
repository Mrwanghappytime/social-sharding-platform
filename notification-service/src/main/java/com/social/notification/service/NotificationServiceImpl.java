package com.social.notification.service;

import com.social.common.api.NotificationService;
import com.social.common.constant.RedisKeys;
import com.social.common.dto.NotificationDTO;
import com.social.common.dto.PageResult;
import com.social.common.entity.Notification;
import com.social.common.enums.NotificationType;
import com.social.common.repository.NotificationRepository;
import io.netty.util.internal.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@DubboService(interfaceClass = NotificationService.class, version = "1.0.0")
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void sendNotification(Long recipientId, NotificationType type, Long actorId, Long targetId,
                                String targetType, String actorUsername, String actorAvatar, boolean isKol) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setType(type);
        notification.setActorId(actorId);
        notification.setTargetId(targetId);
        notification.setTargetType(targetType);
        notification.setActorUsername(actorUsername);
        notification.setActorAvatar(actorAvatar);
        notification.setIsRead(false);

        Notification saved = notificationRepository.save(notification);

        log.info("Notification persisted: recipientId={}, type={}", recipientId, type);

        // For non-KOL users, send real-time notification via Redis Pub/Sub
        if (!isKol) {
            String channel = RedisKeys.notificationChannel(recipientId);
            // Format: id:type:actorId:targetId:targetType:actorUsername:actorAvatar
            String message = saved.getId() + ":" + type.name() + ":" + actorId + ":" + targetId + ":" + targetType + ":" + actorUsername + ":" + (StringUtil.isNullOrEmpty(actorAvatar) ? " " : actorAvatar);
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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        List<NotificationDTO> dtoList = notificationPage.getContent().stream()
                .map(n -> {
                    NotificationDTO dto = new NotificationDTO();
                    dto.setId(n.getId());
                    dto.setType(n.getType().name());
                    dto.setActorId(n.getActorId());
                    dto.setActorUsername(n.getActorUsername());
                    dto.setActorAvatar(n.getActorAvatar());
                    dto.setTargetId(n.getTargetId());
                    dto.setTargetType(n.getTargetType());
                    dto.setIsRead(n.getIsRead());
                    dto.setCreatedAt(n.getCreatedAt() != null ? n.getCreatedAt().format(formatter) : null);
                    return dto;
                })
                .toList();

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
