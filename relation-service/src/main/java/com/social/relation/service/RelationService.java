package com.social.relation.service;

import com.social.relation.dto.RelationCountDTO;
import com.social.relation.dto.UserRelationDTO;

import java.util.List;

public interface RelationService {

    /**
     * Follow a user
     */
    void follow(Long followerId, Long followingId);

    /**
     * Unfollow a user
     */
    void unfollow(Long followerId, Long followingId);

    /**
     * Get following list for a user
     */
    List<UserRelationDTO> getFollowingList(Long userId);

    /**
     * Get followers list for a user
     */
    List<UserRelationDTO> getFollowersList(Long userId);

    /**
     * Get following and follower counts for a user
     */
    RelationCountDTO getRelationCounts(Long userId);

    /**
     * Check if current user follows target user
     */
    boolean isFollowing(Long followerId, Long followingId);
}
