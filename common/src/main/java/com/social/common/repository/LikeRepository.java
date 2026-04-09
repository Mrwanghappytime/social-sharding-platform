package com.social.common.repository;

import com.social.common.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    @Query("SELECT l FROM Like l WHERE l.userId = :userId AND l.postId = :postId")
    Optional<Like> findByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    @Query("SELECT l FROM Like l WHERE l.postId = :postId AND l.userId = :userId")
    Optional<Like> existsByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.postId = :postId")
    long countByPostId(@Param("postId") Long postId);
}
