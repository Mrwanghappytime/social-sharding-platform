package com.social.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 纯净的评论DTO - 只包含自己业务主体的数据
 * 禁止包含: username, userAvatar (这些由facade层负责enrichment)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO implements Serializable {
    private Long id;
    private Long postId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
}
