package com.social.common.repository;

import com.social.common.entity.Follower;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowerRepository extends JpaRepository<Follower, Long> {

    @Query("SELECT f FROM Follower f WHERE f.followerId = :followerId AND f.followingId = :followingId")
    Optional<Follower> findByFollowerIdAndFollowingId(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Query("SELECT f FROM Follower f WHERE f.followingId = :followingId ORDER BY f.createdAt DESC")
    List<Follower> findByFollowingIdOrderByCreatedAtDesc(@Param("followingId") Long followingId);

    @Query("SELECT f FROM Follower f WHERE f.followingId = :followingId ORDER BY f.createdAt DESC")
    Page<Follower> findByFollowingIdOrderByCreatedAtDesc(@Param("followingId") Long followingId, Pageable pageable);

    @Query("SELECT COUNT(f) FROM Follower f WHERE f.followingId = :followingId")
    long countByFollowingId(@Param("followingId") Long followingId);

    @Query("SELECT f.followerId FROM Follower f WHERE f.followingId = :followingId AND f.followerId IN :targetIds")
    List<Long> findFollowerIdsByFollowingIdAndTargetIds(@Param("followingId") Long followingId, @Param("targetIds") List<Long> targetIds);
}
