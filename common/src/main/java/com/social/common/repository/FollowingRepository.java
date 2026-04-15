package com.social.common.repository;

import com.social.common.entity.Following;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowingRepository extends JpaRepository<Following, Long> {

    @Query("SELECT f FROM Following f WHERE f.followerId = :followerId AND f.followingId = :followingId")
    Optional<Following> findByFollowerIdAndFollowingId(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Query("SELECT f FROM Following f WHERE f.followerId = :followerId ORDER BY f.createdAt DESC")
    List<Following> findByFollowerIdOrderByCreatedAtDesc(@Param("followerId") Long followerId);

    @Query("SELECT f FROM Following f WHERE f.followerId = :followerId ORDER BY f.createdAt DESC")
    Page<Following> findByFollowerIdOrderByCreatedAtDesc(@Param("followerId") Long followerId, Pageable pageable);

    @Query("SELECT COUNT(f) FROM Following f WHERE f.followerId = :followerId")
    long countByFollowerId(@Param("followerId") Long followerId);

    @Query("SELECT COUNT(f) FROM Following f WHERE f.followingId = :followingId")
    long countByFollowingId(@Param("followingId") Long followingId);

    @Query("SELECT f.followingId FROM Following f WHERE f.followerId = :followerId AND f.followingId IN :targetIds")
    List<Long> findFollowingIdsByFollowerIdAndTargetIds(@Param("followerId") Long followerId, @Param("targetIds") List<Long> targetIds);
}
