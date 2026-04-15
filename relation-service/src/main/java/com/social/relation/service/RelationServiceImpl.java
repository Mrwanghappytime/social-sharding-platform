package com.social.relation.service;

import com.social.common.api.RelationService;
import com.social.common.dto.PageResult;
import com.social.common.dto.RelationCountDTO;
import com.social.common.dto.UserRelationDTO;
import com.social.common.entity.Follower;
import com.social.common.entity.Following;
import com.social.common.entity.UserRelationCount;
import com.social.common.exception.BusinessException;
import com.social.common.exception.ErrorCode;
import com.social.common.repository.FollowerRepository;
import com.social.common.repository.FollowingRepository;
import com.social.common.repository.UserRelationCountRepository;
import com.social.common.util.LogUtil;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@DubboService(interfaceClass = RelationService.class, version = "1.0.0")
public class RelationServiceImpl implements RelationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RelationServiceImpl.class);

    @Autowired
    private FollowingRepository followingRepository;

    @Autowired
    private FollowerRepository followerRepository;

    @Autowired
    private UserRelationCountRepository userRelationCountRepository;

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

            // Update count table
            updateCountTable(followerId, followingId, 1);

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

            // Update count table
            updateCountTable(followerId, followingId, -1);

            log.info("<<< unfollow EXIT | followerId={} | followingId={} | traceId={}", followerId, followingId, LogUtil.getTraceId());
        } catch (Exception e) {
            log.error("!!! unfollow ERROR | followerId={} | followingId={} | error={}", followerId, followingId, e.getMessage());
            throw e;
        }
    }

    private void updateCountTable(Long followerId, Long followingId, int delta) {
        // Update follower's following count
        UserRelationCount followerCount = userRelationCountRepository.findByUserId(followerId)
                .orElseGet(() -> {
                    UserRelationCount newCount = new UserRelationCount();
                    newCount.setUserId(followerId);
                    newCount.setFollowingCount(0L);
                    newCount.setFollowerCount(0L);
                    return newCount;
                });
        followerCount.setFollowingCount(Math.max(0, followerCount.getFollowingCount() + delta));
        userRelationCountRepository.save(followerCount);

        // Update following's follower count
        UserRelationCount followingCount = userRelationCountRepository.findByUserId(followingId)
                .orElseGet(() -> {
                    UserRelationCount newCount = new UserRelationCount();
                    newCount.setUserId(followingId);
                    newCount.setFollowingCount(0L);
                    newCount.setFollowerCount(0L);
                    return newCount;
                });
        followingCount.setFollowerCount(Math.max(0, followingCount.getFollowerCount() + delta));
        userRelationCountRepository.save(followingCount);
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
    public PageResult<UserRelationDTO> getFollowingListPaged(Long userId, int page, int size) {
        log.debug(">>> getFollowingListPaged ENTER | userId={} | page={} | size={}", userId, page, size);
        try {
            Page<Following> followingPage = followingRepository.findByFollowerIdOrderByCreatedAtDesc(
                    userId, PageRequest.of(page - 1, size));

            List<UserRelationDTO> result = followingPage.getContent().stream()
                    .map(f -> {
                        UserRelationDTO dto = new UserRelationDTO();
                        dto.setUserId(f.getFollowingId());
                        return dto;
                    })
                    .toList();

            PageResult<UserRelationDTO> pageResult = PageResult.of(
                    result,
                    followingPage.getTotalElements(),
                    page,
                    size);
            log.debug("<<< getFollowingListPaged EXIT | userId={} | total={} | traceId={}",
                    userId, followingPage.getTotalElements(), LogUtil.getTraceId());
            return pageResult;
        } catch (Exception e) {
            log.error("!!! getFollowingListPaged ERROR | userId={} | error={}", userId, e.getMessage());
            throw e;
        }
    }

    @Override
    public PageResult<UserRelationDTO> getFollowersListPaged(Long userId, int page, int size) {
        log.debug(">>> getFollowersListPaged ENTER | userId={} | page={} | size={}", userId, page, size);
        try {
            Page<Follower> followerPage = followerRepository.findByFollowingIdOrderByCreatedAtDesc(
                    userId, PageRequest.of(page - 1, size));

            List<UserRelationDTO> result = followerPage.getContent().stream()
                    .map(f -> {
                        UserRelationDTO dto = new UserRelationDTO();
                        dto.setUserId(f.getFollowerId());
                        return dto;
                    })
                    .toList();

            PageResult<UserRelationDTO> pageResult = PageResult.of(
                    result,
                    followerPage.getTotalElements(),
                    page,
                    size);
            log.debug("<<< getFollowersListPaged EXIT | userId={} | total={} | traceId={}",
                    userId, followerPage.getTotalElements(), LogUtil.getTraceId());
            return pageResult;
        } catch (Exception e) {
            log.error("!!! getFollowersListPaged ERROR | userId={} | error={}", userId, e.getMessage());
            throw e;
        }
    }

    @Override
    public RelationCountDTO getRelationCounts(Long userId) {
        log.debug(">>> getRelationCounts ENTER | userId={}", userId);
        try {
            RelationCountDTO counts = new RelationCountDTO();

            Optional<UserRelationCount> countOpt = userRelationCountRepository.findByUserId(userId);
            if (countOpt.isPresent()) {
                UserRelationCount count = countOpt.get();
                counts.setFollowingCount(count.getFollowingCount());
                counts.setFollowerCount(count.getFollowerCount());
            } else {
                // Fallback to count query if record doesn't exist
                long followingCount = followingRepository.countByFollowerId(userId);
                long followerCount = followerRepository.countByFollowingId(userId);
                counts.setFollowingCount(followingCount);
                counts.setFollowerCount(followerCount);
            }

            log.debug("<<< getRelationCounts EXIT | userId={} | following={} | followers={} | traceId={}",
                    userId, counts.getFollowingCount(), counts.getFollowerCount(), LogUtil.getTraceId());
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

    @Override
    public Map<Long, Boolean> areFollowing(Long currentUserId, List<Long> targetUserIds) {
        log.debug(">>> areFollowing ENTER | currentUserId={} | targetIds={}", currentUserId, targetUserIds);
        try {
            if (targetUserIds == null || targetUserIds.isEmpty()) {
                return new HashMap<>();
            }

            List<Long> followingIds = followingRepository.findFollowingIdsByFollowerIdAndTargetIds(
                    currentUserId, targetUserIds);
            Set<Long> followingSet = Set.copyOf(followingIds);

            Map<Long, Boolean> result = targetUserIds.stream()
                    .collect(Collectors.toMap(
                            targetId -> targetId,
                            targetId -> followingSet.contains(targetId)
                    ));

            log.debug("<<< areFollowing EXIT | currentUserId={} | resultSize={} | traceId={}",
                    currentUserId, result.size(), LogUtil.getTraceId());
            return result;
        } catch (Exception e) {
            log.error("!!! areFollowing ERROR | currentUserId={} | error={}", currentUserId, e.getMessage());
            throw e;
        }
    }
}
