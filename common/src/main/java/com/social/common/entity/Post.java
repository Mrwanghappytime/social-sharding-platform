package com.social.common.entity;

import com.social.common.enums.PostType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "posts")
public class Post extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostType type;

    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "comment_count")
    private Integer commentCount = 0;
}
