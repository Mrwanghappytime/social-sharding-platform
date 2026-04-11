package com.social.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelationCountDTO implements Serializable {
    private Long followingCount;
    private Long followerCount;
}
