package com.social.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "followers")
public class Follower extends BaseEntity {

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "following_id", nullable = false)
    private Long followingId;
}
