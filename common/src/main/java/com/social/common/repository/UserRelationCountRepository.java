package com.social.common.repository;

import com.social.common.entity.UserRelationCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRelationCountRepository extends JpaRepository<UserRelationCount, Long> {

    Optional<UserRelationCount> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE UserRelationCount u SET u.followingCount = u.followingCount + 1 WHERE u.userId = :userId")
    void incrementFollowingCount(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserRelationCount u SET u.followerCount = u.followerCount + 1 WHERE u.userId = :userId")
    void incrementFollowerCount(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserRelationCount u SET u.followingCount = u.followingCount - 1 WHERE u.userId = :userId AND u.followingCount > 0")
    void decrementFollowingCount(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserRelationCount u SET u.followerCount = u.followerCount - 1 WHERE u.userId = :userId AND u.followerCount > 0")
    void decrementFollowerCount(@Param("userId") Long userId);
}
