package com.social.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendImageMessageRequest {
    @NotBlank(message = "图片地址不能为空")
    private String imageUrl;

    private String originalImageUrl;
}
