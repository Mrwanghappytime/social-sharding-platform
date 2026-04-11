package com.social.facade.controller;

import com.social.common.api.InteractionService;
import com.social.common.dto.*;
import jakarta.validation.Valid;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/interactions")
public class InteractionFacadeController {

    @DubboReference(version = "1.0.0")
    private InteractionService interactionService;

    @PostMapping("/posts/{postId}/like")
    public Result<Void> likePost(
            @PathVariable(name = "postId") Long postId,
            @RequestHeader("X-User-Id") Long userId) {
        interactionService.likePost(postId, userId);
        return Result.success();
    }

    @DeleteMapping("/posts/{postId}/like")
    public Result<Void> unlikePost(
            @PathVariable(name = "postId") Long postId,
            @RequestHeader("X-User-Id") Long userId) {
        interactionService.unlikePost(postId, userId);
        return Result.success();
    }

    @GetMapping("/posts/{postId}/like")
    public Result<LikeStatusDTO> getLikeStatus(
            @PathVariable(name = "postId") Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        LikeStatusDTO status = interactionService.getLikeStatus(postId, userId);
        return Result.success(status);
    }

    @PostMapping("/posts/{postId}/comments")
    public Result<CommentDTO> commentOnPost(
            @PathVariable(name = "postId") Long postId,
            @Valid @RequestBody CreateCommentRequest createRequest,
            @RequestHeader("X-User-Id") Long userId) {
        CommentDTO comment = interactionService.commentOnPost(postId, userId, createRequest.getContent());
        return Result.success(comment);
    }

    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(
            @PathVariable(name = "id") Long id,
            @RequestHeader("X-User-Id") Long userId) {
        interactionService.deleteComment(id, userId);
        return Result.success();
    }

    @GetMapping("/posts/{postId}/comments")
    public Result<List<CommentDTO>> getComments(
            @PathVariable(name = "postId") Long postId,
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size) {
        List<CommentDTO> comments = interactionService.getComments(postId, page, size);
        return Result.success(comments);
    }
}
