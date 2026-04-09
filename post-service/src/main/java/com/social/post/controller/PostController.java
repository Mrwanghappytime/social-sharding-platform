package com.social.post.controller;

import com.social.common.api.InteractionService;
import com.social.common.api.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.common.dto.*;
import com.social.common.exception.BusinessException;
import com.social.common.exception.ErrorCode;
import com.social.common.util.JwtUtil;
import com.social.post.service.PostServiceImpl;
import jakarta.validation.Valid;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
public class PostController {

    @Autowired
    private PostServiceImpl postService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @DubboReference(version = "1.0.0", check = false)
    private UserService userService;

    @DubboReference(version = "1.0.0", check = false)
    private InteractionService interactionService;

    @PostMapping
    public Result<PostDTO> createPost(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody CreatePostRequest request) {
        Long userId = getUserIdFromToken(token);
        validatePostRequest(request);

        String imageUrlsJson = null;
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            try {
                imageUrlsJson = objectMapper.writeValueAsString(request.getImageUrls());
            } catch (JsonProcessingException e) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "图片URL序列化失败");
            }
        }

        com.social.common.entity.Post post = postService.createPost(
                userId,
                request.getTitle(),
                request.getContent(),
                request.getType(),
                imageUrlsJson,
                request.getVideoUrl()
        );

        PostDTO postDTO = postService.getPostById(post.getId());
        enrichPostDTOWithUserInfo(postDTO);

        return Result.success(postDTO);
    }

    @GetMapping("/{id}")
    public Result<PostDTO> getPostById(
            @PathVariable(name = "id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        PostDTO postDTO = postService.getPostById(id);
        enrichPostDTOWithUserInfo(postDTO);
        Long currentUserId = getCurrentUserIdIfLoggedIn(authorization);
        enrichPostDTOWithLikeStatus(postDTO, currentUserId);
        return Result.success(postDTO);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePost(
            @RequestHeader("Authorization") String token,
            @PathVariable(name = "id") Long id) {
        Long userId = getUserIdFromToken(token);
        postService.deletePost(id, userId);
        return Result.success();
    }

    @GetMapping("/user/{userId}")
    public Result<PageResult<PostDTO>> getUserPosts(
            @PathVariable(name = "userId") Long userId,
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        PageResult<PostDTO> pageResult = postService.getUserPosts(userId, page, size);
        Long currentUserId = getCurrentUserIdIfLoggedIn(authorization);
        for (PostDTO postDTO : pageResult.getRecords()) {
            enrichPostDTOWithUserInfo(postDTO);
            enrichPostDTOWithLikeStatus(postDTO, currentUserId);
        }
        return Result.success(pageResult);
    }

    @GetMapping("/feed")
    public Result<PageResult<PostDTO>> getFeed(
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        PageResult<PostDTO> pageResult = postService.getFeed(page, size);
        Long currentUserId = getCurrentUserIdIfLoggedIn(authorization);
        for (PostDTO postDTO : pageResult.getRecords()) {
            enrichPostDTOWithUserInfo(postDTO);
            enrichPostDTOWithLikeStatus(postDTO, currentUserId);
        }
        return Result.success(pageResult);
    }

    @GetMapping("/search")
    public Result<PageResult<PostDTO>> searchPosts(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        PageResult<PostDTO> pageResult = postService.searchPosts(keyword, page, size);
        Long currentUserId = getCurrentUserIdIfLoggedIn(authorization);
        for (PostDTO postDTO : pageResult.getRecords()) {
            enrichPostDTOWithUserInfo(postDTO);
            enrichPostDTOWithLikeStatus(postDTO, currentUserId);
        }
        return Result.success(pageResult);
    }

    private Long getUserIdFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或token无效");
        }
        String actualToken = token.replace("Bearer ", "");
        if (!jwtUtil.validateToken(actualToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "token已过期或无效");
        }
        return jwtUtil.getUserIdFromToken(actualToken);
    }

    private void validatePostRequest(CreatePostRequest request) {
        if (request.getType() == com.social.common.enums.PostType.IMAGE) {
            if (request.getImageUrls() == null || request.getImageUrls().isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "图片动态必须包含图片");
            }
            if (request.getImageUrls().size() > 9) {
                throw new BusinessException(ErrorCode.IMAGE_COUNT_EXCEEDED, "图片最多9张");
            }
        }
        if (request.getType() == com.social.common.enums.PostType.VIDEO) {
            if (request.getVideoUrl() == null || request.getVideoUrl().isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "视频动态必须包含视频地址");
            }
        }
    }

    private void enrichPostDTOWithUserInfo(PostDTO postDTO) {
        try {
            UserDTO userDTO = userService.getUserById(postDTO.getUserId());
            postDTO.setUsername(userDTO.getUsername());
            postDTO.setUserAvatar(userDTO.getAvatar());
        } catch (Exception e) {
            // If user service is unavailable, set default values
            postDTO.setUsername("未知用户");
            postDTO.setUserAvatar("");
        }
    }

    private Long getCurrentUserIdIfLoggedIn(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String actualToken = authorization.replace("Bearer ", "");
        try {
            if (jwtUtil.validateToken(actualToken)) {
                return jwtUtil.getUserIdFromToken(actualToken);
            }
        } catch (Exception e) {
            // Token invalid, user not logged in
        }
        return null;
    }

    private void enrichPostDTOWithLikeStatus(PostDTO postDTO, Long userId) {
        if (userId == null) {
            postDTO.setIsLiked(false);
            return;
        }
        try {
            var likeStatus = interactionService.getLikeStatus(postDTO.getId(), userId);
            postDTO.setIsLiked(likeStatus.getLiked());
        } catch (Exception e) {
            postDTO.setIsLiked(false);
        }
    }
}
