package com.social.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserDTO implements Serializable {
    private Long id;
    private String username;
    private String avatar;
}
