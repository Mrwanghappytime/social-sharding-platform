package com.social.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 纯净的用户关系DTO - 只包含自己业务主体的数据
 * 禁止包含: username, avatar (这些由facade层负责enrichment)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRelationDTO implements Serializable {
    private Long userId;
}
