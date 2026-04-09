package com.social.user.controller;

import com.social.common.dto.*;
import com.social.common.entity.User;
import com.social.common.util.JwtUtil;
import com.social.user.service.UserServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public Result<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.getUsername(), request.getPassword());
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setAvatar(user.getAvatar());
        return Result.success(userDTO);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setAvatar(user.getAvatar());

        return Result.success(new LoginResponse(token, userDTO));
    }

    @GetMapping("/me")
    public Result<UserDTO> getCurrentUser(@RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        UserDTO userDTO = userService.getUserById(userId);
        return Result.success(userDTO);
    }

    @PutMapping("/avatar")
    public Result<UserDTO> updateAvatar(
            @RequestHeader("Authorization") String token,
            @RequestParam(name = "avatar") String avatar) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        User user = userService.updateAvatar(userId, avatar);

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setAvatar(user.getAvatar());
        return Result.success(userDTO);
    }

    @GetMapping("/{id}")
    public Result<UserDTO> getUserById(@PathVariable(name = "id") Long id) {
        return Result.success(userService.getUserById(id));
    }
}
