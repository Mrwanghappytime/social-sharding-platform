package com.social.facade.controller;

import com.social.common.api.UserService;
import com.social.common.dto.*;
import com.social.common.util.JwtUtil;
import jakarta.validation.Valid;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserFacadeController {

    @DubboReference(version = "1.0.0")
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public Result<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        UserDTO user = userService.register(request.getUsername(), request.getPassword());
        return Result.success(user);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        UserDTO user = userService.login(request.getUsername(), request.getPassword());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return Result.success(new LoginResponse(token, user));
    }

    @GetMapping("/me")
    public Result<UserDTO> getCurrentUser(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(userService.getUserById(userId));
    }

    @PutMapping("/avatar")
    public Result<UserDTO> updateAvatar(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("avatar") String avatar) {
        UserDTO user = userService.updateAvatar(userId, avatar);
        return Result.success(user);
    }

    @GetMapping("/{id}")
    public Result<UserDTO> getUserById(@PathVariable("id") Long id) {
        return Result.success(userService.getUserById(id));
    }
}
