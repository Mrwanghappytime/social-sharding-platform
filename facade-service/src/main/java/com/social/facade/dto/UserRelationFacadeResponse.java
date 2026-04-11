package com.social.facade.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Facade层用户关系响应DTO - 包含enrichment后的完整数据
 * 只包含 userId + enrichment 的 username + avatar
 */
@Data
@NoArgsConstructor
public class UserRelationFacadeResponse implements Serializable {
    private Long userId;
    private String username;
    private String avatar;

    public static UserRelationFacadeResponse of(Long userId, String username, String avatar) {
        UserRelationFacadeResponse response = new UserRelationFacadeResponse();
        response.setUserId(userId);
        response.setUsername(username);
        response.setAvatar(avatar);
        return response;
    }
}
