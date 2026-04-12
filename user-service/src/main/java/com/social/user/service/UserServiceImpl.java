package com.social.user.service;

import com.social.common.api.UserService;
import com.social.common.dto.UserDTO;
import com.social.common.entity.User;
import com.social.common.exception.BusinessException;
import com.social.common.exception.ErrorCode;
import com.social.common.repository.UserRepository;
import com.social.common.util.LogUtil;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@DubboService(version = "1.0.0")
public class UserServiceImpl implements UserService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDTO getUserById(Long userId) {
        log.debug(">>> getUserById ENTER | userId={}", userId);
        try {
            Optional<User> user = userRepository.findById(userId);
            if (user.isEmpty()) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
            }
            UserDTO result = toUserDTO(user.get());
            log.debug("<<< getUserById EXIT | userId={} | traceId={}", userId, LogUtil.getTraceId());
            return result;
        } catch (Exception e) {
            log.error("!!! getUserById ERROR | userId={} | error={}", userId, e.getMessage());
            throw e;
        }
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        log.debug(">>> getUserByUsername ENTER | username={}", username);
        try {
            Optional<User> user = userRepository.findByUsername(username);
            if (user.isEmpty()) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
            }
            UserDTO result = toUserDTO(user.get());
            log.debug("<<< getUserByUsername EXIT | username={} | traceId={}", username, LogUtil.getTraceId());
            return result;
        } catch (Exception e) {
            log.error("!!! getUserByUsername ERROR | username={} | error={}", username, e.getMessage());
            throw e;
        }
    }

    @Override
    public Long getFollowerCount(Long userId) {
        log.debug(">>> getFollowerCount ENTER | userId={}", userId);
        log.debug("<<< getFollowerCount EXIT | userId={} | traceId={}", userId, LogUtil.getTraceId());
        return 0L;
    }

    @Override
    public boolean isUserExists(Long userId) {
        log.debug(">>> isUserExists ENTER | userId={}", userId);
        boolean result = userRepository.existsById(userId);
        log.debug("<<< isUserExists EXIT | userId={} | result={} | traceId={}", userId, result, LogUtil.getTraceId());
        return result;
    }

    @Override
    @Transactional
    public UserDTO register(String username, String password) {
        log.info(">>> register ENTER | username={}", username);
        try {
            if (userRepository.existsByUsername(username)) {
                throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
            }

            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setAvatar("");
            UserDTO result = toUserDTO(userRepository.save(user));
            log.info("<<< register EXIT | username={} | userId={} | traceId={}", username, result.getId(), LogUtil.getTraceId());
            return result;
        } catch (Exception e) {
            log.error("!!! register ERROR | username={} | error={}", username, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public UserDTO login(String username, String password) {
        log.info(">>> login ENTER | username={}", username);
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            User user = userOpt.orElseThrow(() ->
                new BusinessException(ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误"));

            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误");
            }
            UserDTO result = toUserDTO(user);
            log.info("<<< login EXIT | username={} | userId={} | traceId={}", username, result.getId(), LogUtil.getTraceId());
            return result;
        } catch (Exception e) {
            log.error("!!! login ERROR | username={} | error={}", username, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public UserDTO updateAvatar(Long userId, String avatar) {
        log.info(">>> updateAvatar ENTER | userId={} | avatar={}", userId, avatar);
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
            }
            User user = userOpt.get();
            user.setAvatar(avatar);
            UserDTO result = toUserDTO(userRepository.save(user));
            log.info("<<< updateAvatar EXIT | userId={} | traceId={}", userId, LogUtil.getTraceId());
            return result;
        } catch (Exception e) {
            log.error("!!! updateAvatar ERROR | userId={} | error={}", userId, e.getMessage());
            throw e;
        }
    }

    private UserDTO toUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setAvatar(user.getAvatar());
        return dto;
    }
}
