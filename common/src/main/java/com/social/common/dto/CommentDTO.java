package com.social.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CommentDTO implements Serializable {
    private Long id;
    private Long postId;
    private Long userId;
    private String username;
    private String userAvatar;
    private String content;
    private LocalDateTime createdAt;
}
