package com.social.common.dto;

import com.social.common.enums.PostType;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
public class CreatePostRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200")
    private String title;

    private String content;

    @NotNull(message = "动态类型不能为空")
    private PostType type;

    @Size(max = 9, message = "图片最多9张")
    private List<String> imageUrls;

    private String videoUrl;
}
