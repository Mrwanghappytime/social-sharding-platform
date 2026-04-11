package com.social.facade.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.common.api.InteractionService;
import com.social.common.api.PostService;
import com.social.common.api.UserService;
import com.social.common.dto.*;
import com.social.common.enums.PostType;
import com.social.common.exception.BusinessException;
import com.social.common.exception.ErrorCode;
import jakarta.validation.Valid;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/posts")
public class PostFacadeController {

    @DubboReference(version = "1.0.0")
    private PostService postService;

    @DubboReference(version = "1.0.0")
    private UserService userService;

    @DubboReference(version = "1.0.0")
    private InteractionService interactionService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    public Result<PostDTO> createPost(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreatePostRequest request) {
        validatePostRequest(request);

        String imageUrlsJson = null;
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            try {
                imageUrlsJson = objectMapper.writeValueAsString(request.getImageUrls());
            } catch (JsonProcessingException e) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "图片URL序列化失败");
            }
        }

        PostDTO postDTO = postService.createPost(
                userId,
                request.getTitle(),
                request.getContent(),
                request.getType(),
                imageUrlsJson,
                request.getVideoUrl()
        );

        enrichPostDTO(postDTO);
        return Result.success(postDTO);
    }

    @GetMapping("/{id}")
    public Result<PostDTO> getPostById(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        PostDTO postDTO = postService.getPostById(id);
        enrichPostDTO(postDTO);
        if (userId != null) {
            enrichPostDTOWithLikeStatus(postDTO, userId);
        }
        return Result.success(postDTO);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePost(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long id) {
        postService.deletePost(id, userId);
        return Result.success();
    }

    @GetMapping("/user/{userId}")
    public Result<PageResult<PostDTO>> getUserPosts(
            @PathVariable("userId") Long userId,
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        PageResult<PostDTO> pageResult = postService.getUserPosts(userId, page, size);
        for (PostDTO postDTO : pageResult.getRecords()) {
            enrichPostDTO(postDTO);
            if (currentUserId != null) {
                enrichPostDTOWithLikeStatus(postDTO, currentUserId);
            }
        }
        return Result.success(pageResult);
    }

    @GetMapping("/feed")
    public Result<PageResult<PostDTO>> getFeed(
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        PageResult<PostDTO> pageResult = postService.getFeed(page, size);
        for (PostDTO postDTO : pageResult.getRecords()) {
            enrichPostDTO(postDTO);
            if (userId != null) {
                enrichPostDTOWithLikeStatus(postDTO, userId);
            }
        }
        return Result.success(pageResult);
    }

    @GetMapping("/search")
    public Result<PageResult<PostDTO>> searchPosts(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        PageResult<PostDTO> pageResult = postService.searchPosts(keyword, page, size);
        for (PostDTO postDTO : pageResult.getRecords()) {
            enrichPostDTO(postDTO);
            if (userId != null) {
                enrichPostDTOWithLikeStatus(postDTO, userId);
            }
        }
        return Result.success(pageResult);
    }

    private void validatePostRequest(CreatePostRequest request) {
        if (request.getType() == PostType.IMAGE) {
            if (request.getImageUrls() == null || request.getImageUrls().isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "图片动态必须包含图片");
            }
            if (request.getImageUrls().size() > 9) {
                throw new BusinessException(ErrorCode.IMAGE_COUNT_EXCEEDED, "图片最多9张");
            }
        }
        if (request.getType() == PostType.VIDEO) {
            if (request.getVideoUrl() == null || request.getVideoUrl().isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "视频动态必须包含视频地址");
            }
        }
    }

    private void enrichPostDTO(PostDTO postDTO) {
        try {
            UserDTO userDTO = userService.getUserById(postDTO.getUserId());
            postDTO.setUsername(userDTO.getUsername());
            postDTO.setUserAvatar(userDTO.getAvatar());
        } catch (Exception e) {
            postDTO.setUsername("未知用户");
            postDTO.setUserAvatar("");
        }
    }

    private void enrichPostDTOWithLikeStatus(PostDTO postDTO, Long userId) {
        if (userId == null) {
            postDTO.setIsLiked(false);
            return;
        }
        try {
            LikeStatusDTO likeStatus = interactionService.getLikeStatus(postDTO.getId(), userId);
            postDTO.setIsLiked(likeStatus.getLiked());
        } catch (Exception e) {
            postDTO.setIsLiked(false);
        }
    }
}
