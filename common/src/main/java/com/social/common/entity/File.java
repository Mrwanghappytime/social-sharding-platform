package com.social.common.entity;

import com.social.common.enums.MediaType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "files")
public class File extends BaseEntity {

    @Column(name = "post_id")
    private Long postId;

    @Column(nullable = false, length = 500)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaType type;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
