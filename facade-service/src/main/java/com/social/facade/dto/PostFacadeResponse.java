package com.social.facade.dto;

import com.social.common.dto.PostDTO;
import com.social.common.enums.PostType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Facade层帖子响应DTO - 包含enrichment后的完整数据
 * PostDTO 纯净数据 + username + userAvatar + isLiked
 */
@Data
@NoArgsConstructor
public class PostFacadeResponse implements Serializable {
    private Long id;
    private Long userId;
    private String username;
    private String userAvatar;
    private String title;
    private String content;
    private PostType type;
    private Integer likeCount;
    private Integer commentCount;
    private List<String> imageUrls;
    private String videoUrl;
    private Boolean isLiked;
    private LocalDateTime createdAt;

    /**
     * 从纯净的 PostDTO 转换为此 Enrichment Response
     */
    public static PostFacadeResponse fromPostDTO(PostDTO post, String username, String userAvatar, Boolean isLiked) {
        PostFacadeResponse response = new PostFacadeResponse();
        response.setId(post.getId());
        response.setUserId(post.getUserId());
        response.setUsername(username);
        response.setUserAvatar(userAvatar);
        response.setTitle(post.getTitle());
        response.setContent(post.getContent());
        response.setType(post.getType());
        response.setLikeCount(post.getLikeCount());
        response.setCommentCount(post.getCommentCount());
        response.setImageUrls(post.getImageUrls());
        response.setVideoUrl(post.getVideoUrl());
        response.setIsLiked(isLiked);
        response.setCreatedAt(post.getCreatedAt());
        return response;
    }
}
