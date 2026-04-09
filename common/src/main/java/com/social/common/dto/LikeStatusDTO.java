package com.social.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class LikeStatusDTO implements Serializable {
    private Boolean liked;
    private Long likeCount;
}