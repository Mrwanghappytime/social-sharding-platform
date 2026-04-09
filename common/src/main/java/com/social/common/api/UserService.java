package com.social.common.api;

import com.social.common.dto.UserDTO;

public interface UserService {

    UserDTO getUserById(Long userId);

    UserDTO getUserByUsername(String username);

    Long getFollowerCount(Long userId);

    boolean isUserExists(Long userId);
}
