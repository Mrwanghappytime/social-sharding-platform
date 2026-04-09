package com.social.common.repository;

import com.social.common.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostId(Long postId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.postId = :postId ORDER BY c.createdAt ASC")
    List<Comment> findByPostIdOrderByCreatedAtAsc(@Param("postId") Long postId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.postId = :postId")
    long countByPostId(@Param("postId") Long postId);
}
