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
import com.social.facade.dto.PageResultFacadeResponse;
import com.social.facade.dto.PostFacadeResponse;
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
    public Result<PostFacadeResponse> createPost(
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

        PostFacadeResponse response = enrichPostDTO(postDTO, userId);
        return Result.success(response);
    }

    @GetMapping("/{id}")
    public Result<PostFacadeResponse> getPostById(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        PostDTO postDTO = postService.getPostById(id);
        PostFacadeResponse response = enrichPostDTO(postDTO, userId);
        return Result.success(response);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePost(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long id) {
        postService.deletePost(id, userId);
        return Result.success();
    }

    @GetMapping("/user/{userId}")
    public Result<PageResultFacadeResponse<PostFacadeResponse>> getUserPosts(
            @PathVariable("userId") Long userId,
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        PageResult<PostDTO> pageResult = postService.getUserPosts(userId, page, size);

        List<PostFacadeResponse> enrichedPosts = pageResult.getRecords().stream()
                .map(post -> enrichPostDTO(post, currentUserId))
                .toList();

        PageResultFacadeResponse<PostFacadeResponse> response = PageResultFacadeResponse.of(
                enrichedPosts,
                pageResult.getTotal(),
                pageResult.getPage(),
                pageResult.getSize()
        );
        return Result.success(response);
    }

    @GetMapping("/feed")
    public Result<PageResultFacadeResponse<PostFacadeResponse>> getFeed(
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        PageResult<PostDTO> pageResult = postService.getFeed(page, size);

        List<PostFacadeResponse> enrichedPosts = pageResult.getRecords().stream()
                .map(post -> enrichPostDTO(post, userId))
                .toList();

        PageResultFacadeResponse<PostFacadeResponse> response = PageResultFacadeResponse.of(
                enrichedPosts,
                pageResult.getTotal(),
                pageResult.getPage(),
                pageResult.getSize()
        );
        return Result.success(response);
    }

    @GetMapping("/search")
    public Result<PageResultFacadeResponse<PostFacadeResponse>> searchPosts(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        PageResult<PostDTO> pageResult = postService.searchPosts(keyword, page, size);

        List<PostFacadeResponse> enrichedPosts = pageResult.getRecords().stream()
                .map(post -> enrichPostDTO(post, userId))
                .toList();

        PageResultFacadeResponse<PostFacadeResponse> response = PageResultFacadeResponse.of(
                enrichedPosts,
                pageResult.getTotal(),
                pageResult.getPage(),
                pageResult.getSize()
        );
        return Result.success(response);
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

    private PostFacadeResponse enrichPostDTO(PostDTO postDTO, Long currentUserId) {
        String username = "未知用户";
        String userAvatar = "";
        Boolean isLiked = false;

        try {
            UserDTO userDTO = userService.getUserById(postDTO.getUserId());
            username = userDTO.getUsername() != null ? userDTO.getUsername() : "未知用户";
            userAvatar = userDTO.getAvatar() != null ? userDTO.getAvatar() : "";
        } catch (Exception e) {
            // Use defaults
        }

        if (currentUserId != null) {
            try {
                LikeStatusDTO likeStatus = interactionService.getLikeStatus(postDTO.getId(), currentUserId);
                isLiked = likeStatus.getLiked();
            } catch (Exception e) {
                isLiked = false;
            }
        }

        return PostFacadeResponse.fromPostDTO(postDTO, username, userAvatar, isLiked);
    }
}
