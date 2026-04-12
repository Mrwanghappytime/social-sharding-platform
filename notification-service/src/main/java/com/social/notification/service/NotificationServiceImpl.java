package com.social.notification.service;

import com.social.common.api.NotificationService;
import com.social.common.constant.RedisKeys;
import com.social.common.dto.NotificationDTO;
import com.social.common.dto.PageResult;
import com.social.common.entity.Notification;
import com.social.common.enums.NotificationType;
import com.social.common.repository.NotificationRepository;
import com.social.common.util.LogUtil;
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
        log.info(">>> sendNotification ENTER | recipientId={} | type={} | actorId={}", recipientId, type, actorId);
        try {
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

            log.info("Notification persisted: recipientId={}, type={}, notificationId={}", recipientId, type, saved.getId());

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
            log.info("<<< sendNotification EXIT | recipientId={} | notificationId={} | traceId={}", recipientId, saved.getId(), LogUtil.getTraceId());
        } catch (Exception e) {
            log.error("!!! sendNotification ERROR | recipientId={} | error={}", recipientId, e.getMessage());
            throw e;
        }
    }

    @Override
    public PageResult<NotificationDTO> getNotificationList(Long recipientId, Integer page, Integer size) {
        log.debug(">>> getNotificationList ENTER | recipientId={} | page={} | size={}", recipientId, page, size);
        try {
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

            PageResult<NotificationDTO> result = PageResult.of(dtoList, notificationPage.getTotalElements(), page, size);
            log.debug("<<< getNotificationList EXIT | recipientId={} | total={} | traceId={}", recipientId, result.getTotal(), LogUtil.getTraceId());
            return result;
        } catch (Exception e) {
            log.error("!!! getNotificationList ERROR | recipientId={} | error={}", recipientId, e.getMessage());
            throw e;
        }
    }

    public void markAsRead(Long id, Long recipientId) {
        log.debug(">>> markAsRead ENTER | id={} | recipientId={}", id, recipientId);
        notificationRepository.findById(id).ifPresent(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
        log.info("<<< markAsRead EXIT | id={} | recipientId={} | traceId={}", id, recipientId, LogUtil.getTraceId());
    }

    public void markAllAsRead(Long recipientId) {
        log.debug(">>> markAllAsRead ENTER | recipientId={}", recipientId);
        List<Notification> unreadNotifications = notificationRepository.findUnreadByRecipientId(recipientId);
        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
        log.info("<<< markAllAsRead EXIT | recipientId={} | count={} | traceId={}", recipientId, unreadNotifications.size(), LogUtil.getTraceId());
    }

    public Long getUnreadCount(Long recipientId) {
        log.debug(">>> getUnreadCount ENTER | recipientId={}", recipientId);
        Long count = notificationRepository.countUnreadByRecipientId(recipientId);
        log.debug("<<< getUnreadCount EXIT | recipientId={} | count={} | traceId={}", recipientId, count, LogUtil.getTraceId());
        return count;
    }
}
