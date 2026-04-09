package com.social.common.dto;

import com.social.common.enums.PostType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostDTO {
    private Long id;
    private Long userId;
    private String username;
    private String userAvatar;
    private String title;
    private String content;
    private PostType type;
    private Integer likeCount;
    private Integer commentCount;
    private List<FileDTO> mediaFiles;
    private List<String> imageUrls;
    private String videoUrl;
    private Boolean isLiked;
    private LocalDateTime createdAt;
}
