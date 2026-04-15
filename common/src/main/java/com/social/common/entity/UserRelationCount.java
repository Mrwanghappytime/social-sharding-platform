package com.social.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user_relation_count")
public class UserRelationCount extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "following_count", nullable = false)
    private Long followingCount = 0L;

    @Column(name = "follower_count", nullable = false)
    private Long followerCount = 0L;
}
