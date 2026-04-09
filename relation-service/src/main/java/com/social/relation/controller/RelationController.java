package com.social.relation.controller;

import com.social.common.dto.Result;
import com.social.common.util.JwtUtil;
import com.social.relation.dto.RelationCountDTO;
import com.social.relation.dto.UserRelationDTO;
import com.social.relation.service.RelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/relations")
public class RelationController {

    @Autowired
    private RelationService relationService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Follow a user
     */
    @PostMapping("/follow/{userId}")
    public Result<Void> follow(@PathVariable(name = "userId") Long userId,
                                @RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long currentUserId = jwtUtil.getUserIdFromToken(actualToken);
        relationService.follow(currentUserId, userId);
        return Result.success();
    }

    /**
     * Unfollow a user
     */
    @DeleteMapping("/follow/{userId}")
    public Result<Void> unfollow(@PathVariable(name = "userId") Long userId,
                                  @RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long currentUserId = jwtUtil.getUserIdFromToken(actualToken);
        relationService.unfollow(currentUserId, userId);
        return Result.success();
    }

    /**
     * Get my following list
     */
    @GetMapping("/following")
    public Result<List<UserRelationDTO>> getMyFollowingList(@RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long currentUserId = jwtUtil.getUserIdFromToken(actualToken);
        return Result.success(relationService.getFollowingList(currentUserId));
    }

    /**
     * Get my followers list
     */
    @GetMapping("/followers")
    public Result<List<UserRelationDTO>> getMyFollowersList(@RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long currentUserId = jwtUtil.getUserIdFromToken(actualToken);
        return Result.success(relationService.getFollowersList(currentUserId));
    }

    /**
     * Get a user's following list
     */
    @GetMapping("/following/{userId}")
    public Result<List<UserRelationDTO>> getFollowingList(@PathVariable(name = "userId") Long userId) {
        return Result.success(relationService.getFollowingList(userId));
    }

    /**
     * Get a user's followers list
     */
    @GetMapping("/followers/{userId}")
    public Result<List<UserRelationDTO>> getFollowersList(@PathVariable(name = "userId") Long userId) {
        return Result.success(relationService.getFollowersList(userId));
    }

    /**
     * Get following and follower counts for a user
     */
    @GetMapping("/counts/{userId}")
    public Result<RelationCountDTO> getRelationCounts(@PathVariable(name = "userId") Long userId) {
        return Result.success(relationService.getRelationCounts(userId));
    }

    /**
     * Check if I follow a user
     */
    @GetMapping("/is-following/{userId}")
    public Result<Boolean> isFollowing(@PathVariable(name = "userId") Long userId,
                                       @RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long currentUserId = jwtUtil.getUserIdFromToken(actualToken);
        return Result.success(relationService.isFollowing(currentUserId, userId));
    }
}
