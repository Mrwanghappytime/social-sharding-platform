package com.social.facade.dto;

import com.social.common.dto.CommentDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Facade层评论响应DTO - 包含enrichment后的完整数据
 * CommentDTO 纯净数据 + username + userAvatar
 */
@Data
@NoArgsConstructor
public class CommentFacadeResponse implements Serializable {
    private Long id;
    private Long postId;
    private Long userId;
    private String username;
    private String userAvatar;
    private String content;
    private LocalDateTime createdAt;

    /**
     * 从纯净的 CommentDTO 转换为此 Enrichment Response
     */
    public static CommentFacadeResponse fromCommentDTO(CommentDTO comment, String username, String userAvatar) {
        CommentFacadeResponse response = new CommentFacadeResponse();
        response.setId(comment.getId());
        response.setPostId(comment.getPostId());
        response.setUserId(comment.getUserId());
        response.setUsername(username);
        response.setUserAvatar(userAvatar);
        response.setContent(comment.getContent());
        response.setCreatedAt(comment.getCreatedAt());
        return response;
    }
}
