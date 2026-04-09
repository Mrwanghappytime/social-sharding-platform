package com.social.common.repository;

import com.social.common.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientId(Long recipientId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId ORDER BY n.createdAt DESC")
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(@Param("recipientId") Long recipientId);

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByRecipientId(@Param("recipientId") Long recipientId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientId = :recipientId AND n.isRead = false")
    long countUnreadByRecipientId(@Param("recipientId") Long recipientId);
}
