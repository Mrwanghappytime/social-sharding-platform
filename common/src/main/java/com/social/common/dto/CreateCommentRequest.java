package com.social.common.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CreateCommentRequest {

    @NotNull(message = "动态ID不能为空")
    private Long postId;

    @NotBlank(message = "评论内容不能为空")
    private String content;
}
