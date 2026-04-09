package com.social.interaction.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentDTO {
    private Long id;
    private Long postId;
    private Long userId;
    private String username;
    private String userAvatar;
    private String content;
    private LocalDateTime createdAt;
}
