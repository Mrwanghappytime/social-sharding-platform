package com.social.user.service;

import com.social.common.api.UserService;
import com.social.common.dto.UserDTO;
import com.social.common.entity.User;
import com.social.common.exception.BusinessException;
import com.social.common.exception.ErrorCode;
import com.social.common.repository.UserRepository;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@DubboService(version = "1.0.0")
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDTO getUserById(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        return toUserDTO(user.get());
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        return toUserDTO(user.get());
    }

    @Override
    public Long getFollowerCount(Long userId) {
        return 0L;
    }

    @Override
    public boolean isUserExists(Long userId) {
        return userRepository.existsById(userId);
    }

    @Transactional
    public User register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setAvatar("");
        return userRepository.save(user);
    }

    @Transactional
    public User login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        User user = userOpt.orElseThrow(() ->
            new BusinessException(ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误");
        }
        return user;
    }

    @Transactional
    public User updateAvatar(Long userId, String avatar) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        User user = userOpt.get();
        user.setAvatar(avatar);
        return userRepository.save(user);
    }

    private UserDTO toUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setAvatar(user.getAvatar());
        return dto;
    }
}
