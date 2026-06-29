package com.social.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateConversationRequest {
    @NotNull(message = "目标用户不能为空")
    private Long targetUserId;
}
