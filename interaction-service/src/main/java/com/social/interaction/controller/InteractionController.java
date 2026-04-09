package com.social.interaction.controller;

import com.social.common.dto.Result;
import com.social.common.exception.BusinessException;
import com.social.common.exception.ErrorCode;
import com.social.common.util.JwtUtil;
import com.social.common.dto.CommentDTO;
import com.social.interaction.dto.CreateCommentRequest;
import com.social.common.dto.LikeStatusDTO;
import com.social.common.api.InteractionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/interactions")
public class InteractionController {

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private JwtUtil jwtUtil;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Like a post
     */
    @PostMapping("/posts/{postId}/like")
    public Result<Void> likePost(@PathVariable(name = "postId") Long postId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        interactionService.likePost(postId, userId);
        return Result.success();
    }

    /**
     * Unlike a post
     */
    @DeleteMapping("/posts/{postId}/like")
    public Result<Void> unlikePost(@PathVariable(name = "postId") Long postId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        interactionService.unlikePost(postId, userId);
        return Result.success();
    }

    /**
     * Get like status and count for a post
     */
    @GetMapping("/posts/{postId}/like")
    public Result<LikeStatusDTO> getLikeStatus(@PathVariable(name = "postId") Long postId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        LikeStatusDTO status = interactionService.getLikeStatus(postId, userId);
        return Result.success(status);
    }

    /**
     * Comment on a post
     */
    @PostMapping("/posts/{postId}/comments")
    public Result<CommentDTO> commentOnPost(@PathVariable(name = "postId") Long postId,
                                            @RequestBody CreateCommentRequest createRequest,
                                            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        CommentDTO comment = interactionService.commentOnPost(postId, userId, createRequest.getContent());
        return Result.success(comment);
    }

    /**
     * Delete a comment
     */
    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable(name = "id") Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        interactionService.deleteComment(id, userId);
        return Result.success();
    }

    /**
     * Get comments for a post
     */
    @GetMapping("/posts/{postId}/comments")
    public Result<List<CommentDTO>> getComments(@PathVariable(name = "postId") Long postId,
                                                  @RequestParam(name = "page", defaultValue = "1") Integer page,
                                                  @RequestParam(name = "size", defaultValue = "10") Integer size) {
        List<CommentDTO> comments = interactionService.getComments(postId, page, size);
        return Result.success(comments);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未授权，请先登录");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的token");
        }
    }
}
