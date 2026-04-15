package com.social.common.api;

import com.social.common.dto.PageResult;
import com.social.common.dto.RelationCountDTO;
import com.social.common.dto.UserRelationDTO;

import java.util.List;
import java.util.Map;

public interface RelationService {

    void follow(Long followerId, Long followingId);

    void unfollow(Long followerId, Long followingId);

    List<UserRelationDTO> getFollowingList(Long userId);

    List<UserRelationDTO> getFollowersList(Long userId);

    PageResult<UserRelationDTO> getFollowingListPaged(Long userId, int page, int size);

    PageResult<UserRelationDTO> getFollowersListPaged(Long userId, int page, int size);

    RelationCountDTO getRelationCounts(Long userId);

    boolean isFollowing(Long followerId, Long followingId);

    Map<Long, Boolean> areFollowing(Long currentUserId, List<Long> targetUserIds);
}
