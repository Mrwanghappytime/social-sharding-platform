package com.social.common.api;

import com.social.common.dto.RelationCountDTO;
import com.social.common.dto.UserRelationDTO;

import java.util.List;

public interface RelationService {

    void follow(Long followerId, Long followingId);

    void unfollow(Long followerId, Long followingId);

    List<UserRelationDTO> getFollowingList(Long userId);

    List<UserRelationDTO> getFollowersList(Long userId);

    RelationCountDTO getRelationCounts(Long userId);

    boolean isFollowing(Long followerId, Long followingId);
}
