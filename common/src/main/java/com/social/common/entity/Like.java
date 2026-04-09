package com.social.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"post_id", "user_id"})
})
public class Like extends BaseEntity {

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
