package com.social.relation.service;

import com.social.common.api.RelationService;
import com.social.common.dto.RelationCountDTO;
import com.social.common.dto.UserRelationDTO;
import com.social.common.entity.Follower;
import com.social.common.entity.Following;
import com.social.common.exception.BusinessException;
import com.social.common.exception.ErrorCode;
import com.social.common.repository.FollowerRepository;
import com.social.common.repository.FollowingRepository;
import com.social.common.util.LogUtil;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@DubboService(interfaceClass = RelationService.class, version = "1.0.0")
public class RelationServiceImpl implements RelationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RelationServiceImpl.class);

    @Autowired
    private FollowingRepository followingRepository;

    @Autowired
    private FollowerRepository followerRepository;

    @Override
    @Transactional
    public void follow(Long followerId, Long followingId) {
        log.info(">>> follow ENTER | followerId={} | followingId={}", followerId, followingId);
        try {
            if (followerId.equals(followingId)) {
                throw new BusinessException(ErrorCode.CANNOT_FOLLOW_SELF, "不能关注自己");
            }

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

            log.info("<<< follow EXIT | followerId={} | followingId={} | traceId={}", followerId, followingId, LogUtil.getTraceId());
        } catch (Exception e) {
            log.error("!!! follow ERROR | followerId={} | followingId={} | error={}", followerId, followingId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        log.info(">>> unfollow ENTER | followerId={} | followingId={}", followerId, followingId);
        try {
            Optional<Following> existing = followingRepository.findByFollowerIdAndFollowingId(followerId, followingId);
            if (existing.isEmpty()) {
                throw new BusinessException(ErrorCode.NOT_FOLLOWING, "未关注该用户");
            }

            followingRepository.delete(existing.get());

            Optional<Follower> followerExisting = followerRepository.findByFollowerIdAndFollowingId(followerId, followingId);
            followerExisting.ifPresent(followerRepository::delete);

            log.info("<<< unfollow EXIT | followerId={} | followingId={} | traceId={}", followerId, followingId, LogUtil.getTraceId());
        } catch (Exception e) {
            log.error("!!! unfollow ERROR | followerId={} | followingId={} | error={}", followerId, followingId, e.getMessage());
            throw e;
        }
    }

    @Override
    public List<UserRelationDTO> getFollowingList(Long userId) {
        log.debug(">>> getFollowingList ENTER | userId={}", userId);
        try {
            List<Following> followingList = followingRepository.findByFollowerIdOrderByCreatedAtDesc(userId);

            List<UserRelationDTO> result = followingList.stream()
                    .map(f -> {
                        UserRelationDTO dto = new UserRelationDTO();
                        dto.setUserId(f.getFollowingId());
                        return dto;
                    })
                    .toList();
            log.debug("<<< getFollowingList EXIT | userId={} | count={} | traceId={}", userId, result.size(), LogUtil.getTraceId());
            return result;
        } catch (Exception e) {
            log.error("!!! getFollowingList ERROR | userId={} | error={}", userId, e.getMessage());
            throw e;
        }
    }

    @Override
    public List<UserRelationDTO> getFollowersList(Long userId) {
        log.debug(">>> getFollowersList ENTER | userId={}", userId);
        try {
            List<Follower> followerList = followerRepository.findByFollowingIdOrderByCreatedAtDesc(userId);

            List<UserRelationDTO> result = followerList.stream()
                    .map(f -> {
                        UserRelationDTO dto = new UserRelationDTO();
                        dto.setUserId(f.getFollowerId());
                        return dto;
                    })
                    .toList();
            log.debug("<<< getFollowersList EXIT | userId={} | count={} | traceId={}", userId, result.size(), LogUtil.getTraceId());
            return result;
        } catch (Exception e) {
            log.error("!!! getFollowersList ERROR | userId={} | error={}", userId, e.getMessage());
            throw e;
        }
    }

    @Override
    public RelationCountDTO getRelationCounts(Long userId) {
        log.debug(">>> getRelationCounts ENTER | userId={}", userId);
        try {
            RelationCountDTO counts = new RelationCountDTO();
            long followingCount = followingRepository.countByFollowerId(userId);
            long followerCount = followerRepository.countByFollowingId(userId);
            counts.setFollowingCount(followingCount);
            counts.setFollowerCount(followerCount);
            log.debug("<<< getRelationCounts EXIT | userId={} | following={} | followers={} | traceId={}",
                    userId, followingCount, followerCount, LogUtil.getTraceId());
            return counts;
        } catch (Exception e) {
            log.error("!!! getRelationCounts ERROR | userId={} | error={}", userId, e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        log.debug(">>> isFollowing ENTER | followerId={} | followingId={}", followerId, followingId);
        boolean result = followingRepository.findByFollowerIdAndFollowingId(followerId, followingId).isPresent();
        log.debug("<<< isFollowing EXIT | followerId={} | followingId={} | result={} | traceId={}",
                followerId, followingId, result, LogUtil.getTraceId());
        return result;
    }
}
