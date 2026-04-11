package com.social.facade.controller;

import com.social.common.api.RelationService;
import com.social.common.dto.RelationCountDTO;
import com.social.common.dto.Result;
import com.social.common.dto.UserRelationDTO;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/relations")
public class RelationFacadeController {

    @DubboReference(version = "1.0.0")
    private RelationService relationService;

    @PostMapping("/follow/{userId}")
    public Result<Void> follow(
            @PathVariable(name = "userId") Long userId,
            @RequestHeader("X-User-Id") Long currentUserId) {
        relationService.follow(currentUserId, userId);
        return Result.success();
    }

    @DeleteMapping("/follow/{userId}")
    public Result<Void> unfollow(
            @PathVariable(name = "userId") Long userId,
            @RequestHeader("X-User-Id") Long currentUserId) {
        relationService.unfollow(currentUserId, userId);
        return Result.success();
    }

    @GetMapping("/following")
    public Result<List<UserRelationDTO>> getMyFollowingList(
            @RequestHeader("X-User-Id") Long currentUserId) {
        return Result.success(relationService.getFollowingList(currentUserId));
    }

    @GetMapping("/followers")
    public Result<List<UserRelationDTO>> getMyFollowersList(
            @RequestHeader("X-User-Id") Long currentUserId) {
        return Result.success(relationService.getFollowersList(currentUserId));
    }

    @GetMapping("/following/{userId}")
    public Result<List<UserRelationDTO>> getFollowingList(@PathVariable(name = "userId") Long userId) {
        return Result.success(relationService.getFollowingList(userId));
    }

    @GetMapping("/followers/{userId}")
    public Result<List<UserRelationDTO>> getFollowersList(@PathVariable(name = "userId") Long userId) {
        return Result.success(relationService.getFollowersList(userId));
    }

    @GetMapping("/counts/{userId}")
    public Result<RelationCountDTO> getRelationCounts(@PathVariable(name = "userId") Long userId) {
        return Result.success(relationService.getRelationCounts(userId));
    }

    @GetMapping("/is-following/{userId}")
    public Result<Boolean> isFollowing(
            @PathVariable(name = "userId") Long userId,
            @RequestHeader("X-User-Id") Long currentUserId) {
        return Result.success(relationService.isFollowing(currentUserId, userId));
    }
}
