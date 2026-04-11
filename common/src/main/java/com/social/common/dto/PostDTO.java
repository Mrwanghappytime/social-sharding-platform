package com.social.common.dto;

import com.social.common.enums.PostType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 纯净的帖子DTO - 只包含自己业务主体的数据
 * 禁止包含: username, userAvatar, isLiked, mediaFiles (这些由facade层负责enrichment)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO implements Serializable {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private PostType type;
    private Integer likeCount;
    private Integer commentCount;
    private List<String> imageUrls;
    private String videoUrl;
    private LocalDateTime createdAt;
}
