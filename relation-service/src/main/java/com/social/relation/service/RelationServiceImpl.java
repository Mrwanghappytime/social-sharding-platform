package com.social.relation.service;

import com.social.common.api.NotificationService;
import com.social.common.api.UserService;
import com.social.common.entity.Follower;
import com.social.common.entity.Following;
import com.social.common.enums.NotificationType;
import com.social.common.exception.BusinessException;
import com.social.common.exception.ErrorCode;
import com.social.common.repository.FollowerRepository;
import com.social.common.repository.FollowingRepository;
import com.social.relation.dto.RelationCountDTO;
import com.social.relation.dto.UserRelationDTO;
import com.social.relation.util.TableShardingUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RelationServiceImpl implements RelationService {

    @Autowired
    private FollowingRepository followingRepository;

    @Autowired
    private FollowerRepository followerRepository;

    @DubboReference(version = "1.0.0", check = false)
    private UserService userService;

    @DubboReference(version = "1.0.0", check = false)
    private NotificationService notificationService;

    @Override
    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException(ErrorCode.CANNOT_FOLLOW_SELF, "不能关注自己");
        }

        if (!userService.isUserExists(followingId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        int followingTableIndex = TableShardingUtil.getFollowingTableIndex(followerId);
        int followersTableIndex = TableShardingUtil.getFollowersTableIndex(followingId);

        Optional<Following> existing = followingRepository.findByFollowerIdAndFollowingId(followerId, followingId);
        if (existing.isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_FOLLOWING, "已经关注过该用户");
        }

        Following following = new Following();
        following.setFollowerId(followerId);
        following.setFollowingId(followingId);
        followingRepository.save(following);

        Follower follower = new Follower();
        follower.setFollowerId(followerId);
        follower.setFollowingId(followingId);
        followerRepository.save(follower);

        // Send follow notification
        notificationService.sendNotification(followingId, NotificationType.FOLLOW, followerId, followingId, "USER");
    }

    @Override
    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        Optional<Following> existing = followingRepository.findByFollowerIdAndFollowingId(followerId, followingId);
        if (existing.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOLLOWING, "未关注该用户");
        }

        followingRepository.delete(existing.get());

        Optional<Follower> followerExisting = followerRepository.findByFollowerIdAndFollowingId(followerId, followingId);
        followerExisting.ifPresent(followerRepository::delete);
    }

    @Override
    public List<UserRelationDTO> getFollowingList(Long userId) {
        List<Following> followingList = followingRepository.findByFollowerIdOrderByCreatedAtDesc(userId);

        return followingList.stream()
                .map(f -> {
                    UserRelationDTO dto = new UserRelationDTO();
                    dto.setUserId(f.getFollowingId());
                    try {
                        var user = userService.getUserById(f.getFollowingId());
                        dto.setUsername(user.getUsername());
                        dto.setAvatar(user.getAvatar());
                    } catch (Exception e) {
                        dto.setUsername("unknown");
                        dto.setAvatar("");
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<UserRelationDTO> getFollowersList(Long userId) {
        List<Follower> followerList = followerRepository.findByFollowingIdOrderByCreatedAtDesc(userId);

        return followerList.stream()
                .map(f -> {
                    UserRelationDTO dto = new UserRelationDTO();
                    dto.setUserId(f.getFollowerId());
                    try {
                        var user = userService.getUserById(f.getFollowerId());
                        dto.setUsername(user.getUsername());
                        dto.setAvatar(user.getAvatar());
                    } catch (Exception e) {
                        dto.setUsername("unknown");
                        dto.setAvatar("");
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public RelationCountDTO getRelationCounts(Long userId) {
        RelationCountDTO counts = new RelationCountDTO();
        long followingCount = followingRepository.countByFollowerId(userId);
        long followerCount = followerRepository.countByFollowingId(userId);
        counts.setFollowingCount(followingCount);
        counts.setFollowerCount(followerCount);
        return counts;
    }

    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        return followingRepository.findByFollowerIdAndFollowingId(followerId, followingId).isPresent();
    }
}
