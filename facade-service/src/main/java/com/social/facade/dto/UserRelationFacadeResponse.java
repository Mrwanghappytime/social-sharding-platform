package com.social.facade.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Facade层用户关系响应DTO - 包含enrichment后的完整数据
 */
@Data
@NoArgsConstructor
public class UserRelationFacadeResponse implements Serializable {
    private Long userId;
    private String username;
    private String avatar;
    private Long followingCount;
    private Long followersCount;
    private Boolean isFollowing;

    public static UserRelationFacadeResponse of(Long userId, String username, String avatar,
                                                Long followingCount, Long followersCount, Boolean isFollowing) {
        UserRelationFacadeResponse response = new UserRelationFacadeResponse();
        response.setUserId(userId);
        response.setUsername(username);
        response.setAvatar(avatar);
        response.setFollowingCount(followingCount);
        response.setFollowersCount(followersCount);
        response.setIsFollowing(isFollowing);
        return response;
    }
}
