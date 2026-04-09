package com.social.common.dto;

import com.social.common.enums.MediaType;
import lombok.Data;

@Data
public class FileDTO {
    private Long id;
    private String url;
    private MediaType type;
    private Integer sortOrder;
}
