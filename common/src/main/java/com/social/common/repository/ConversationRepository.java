package com.social.common.repository;

import com.social.common.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);

    @Query("SELECT c FROM Conversation c WHERE c.user1Id = :userId OR c.user2Id = :userId ORDER BY c.updatedAt DESC")
    Page<Conversation> findByParticipant(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.id IN :ids AND (c.user1Id = :userId OR c.user2Id = :userId)")
    List<Conversation> findByIdsAndParticipant(@Param("ids") List<Long> ids, @Param("userId") Long userId);
}
