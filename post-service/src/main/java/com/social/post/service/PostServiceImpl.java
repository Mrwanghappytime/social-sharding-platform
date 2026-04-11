package com.social.post.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.common.api.PostService;
import com.social.common.dto.PostDTO;
import com.social.common.dto.PageResult;
import com.social.common.entity.Post;
import com.social.common.exception.BusinessException;
import com.social.common.exception.ErrorCode;
import com.social.common.repository.PostRepository;
import com.social.common.util.LogUtil;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@DubboService(version = "1.0.0")
public class PostServiceImpl implements PostService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PostServiceImpl.class);

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String POST_CACHE_KEY = "post:";
    private static final String USER_POSTS_KEY = "user:posts:";

    @Override
    public PostDTO getPostById(Long postId) {
        log.debug(">>> getPostById ENTER | postId={}", postId);
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND, "动态不存在"));
            PostDTO result = toPostDTO(post);
            log.debug("<<< getPostById EXIT | postId={} | traceId={}", postId, LogUtil.getTraceId());
            return result;
        } catch (Exception e) {
            log.error("!!! getPostById ERROR | postId={} | error={}", postId, e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean isPostExists(Long postId) {
        log.debug(">>> isPostExists ENTER | postId={}", postId);
        boolean result = postRepository.existsById(postId);
        log.debug("<<< isPostExists EXIT | postId={} | result={} | traceId={}", postId, result, LogUtil.getTraceId());
        return result;
    }

    @Override
    public Long getUserIdByPostId(Long postId) {
        log.debug(">>> getUserIdByPostId ENTER | postId={}", postId);
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND, "动态不存在"));
            Long userId = post.getUserId();
            log.debug("<<< getUserIdByPostId EXIT | postId={} | userId={} | traceId={}", postId, userId, LogUtil.getTraceId());
            return userId;
        } catch (Exception e) {
            log.error("!!! getUserIdByPostId ERROR | postId={} | error={}", postId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void incrementLikeCount(Long postId) {
        log.debug(">>> incrementLikeCount ENTER | postId={}", postId);
        try {
            postRepository.findById(postId).ifPresent(post -> {
                post.setLikeCount(post.getLikeCount() == null ? 1 : post.getLikeCount() + 1);
                postRepository.save(post);
            });
            log.debug("<<< incrementLikeCount EXIT | postId={} | traceId={}", postId, LogUtil.getTraceId());
        } catch (Exception e) {
            log.error("!!! incrementLikeCount ERROR | postId={} | error={}", postId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void decrementLikeCount(Long postId) {
        log.debug(">>> decrementLikeCount ENTER | postId={}", postId);
        try {
            postRepository.findById(postId).ifPresent(post -> {
                post.setLikeCount(post.getLikeCount() == null || post.getLikeCount() <= 0 ? 0 : post.getLikeCount() - 1);
                postRepository.save(post);
            });
            log.debug("<<< decrementLikeCount EXIT | postId={} | traceId={}", postId, LogUtil.getTraceId());
        } catch (Exception e) {
            log.error("!!! decrementLikeCount ERROR | postId={} | error={}", postId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void incrementCommentCount(Long postId) {
        log.debug(">>> incrementCommentCount ENTER | postId={}", postId);
        try {
            postRepository.findById(postId).ifPresent(post -> {
                post.setCommentCount(post.getCommentCount() == null ? 1 : post.getCommentCount() + 1);
                postRepository.save(post);
            });
            log.debug("<<< incrementCommentCount EXIT | postId={} | traceId={}", postId, LogUtil.getTraceId());
        } catch (Exception e) {
            log.error("!!! incrementCommentCount ERROR | postId={} | error={}", postId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void decrementCommentCount(Long postId) {
        log.debug(">>> decrementCommentCount ENTER | postId={}", postId);
        try {
            postRepository.findById(postId).ifPresent(post -> {
                post.setCommentCount(post.getCommentCount() == null || post.getCommentCount() <= 0 ? 0 : post.getCommentCount() - 1);
                postRepository.save(post);
            });
            log.debug("<<< decrementCommentCount EXIT | postId={} | traceId={}", postId, LogUtil.getTraceId());
        } catch (Exception e) {
            log.error("!!! decrementCommentCount ERROR | postId={} | error={}", postId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public PostDTO createPost(Long userId, String title, String content, com.social.common.enums.PostType type, String imageUrls, String videoUrl) {
        log.info(">>> createPost ENTER | userId={} | title={}", userId, title);
        try {
            Post post = new Post();
            post.setUserId(userId);
            post.setTitle(title);
            post.setContent(content);
            post.setType(type);
            post.setImageUrls(imageUrls);
            post.setVideoUrl(videoUrl);
            Post savedPost = postRepository.save(post);

            redisTemplate.delete(USER_POSTS_KEY + userId);
            redisTemplate.delete("post:feed");

            PostDTO result = toPostDTO(savedPost);
            log.info("<<< createPost EXIT | userId={} | postId={} | traceId={}", userId, result.getId(), LogUtil.getTraceId());
            return result;
        } catch (Exception e) {
            log.error("!!! createPost ERROR | userId={} | error={}", userId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        log.info(">>> deletePost ENTER | postId={} | userId={}", postId, userId);
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND, "动态不存在"));

            if (!post.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除此动态");
            }

            postRepository.deleteById(postId);

            redisTemplate.delete(POST_CACHE_KEY + postId);
            redisTemplate.delete(USER_POSTS_KEY + userId);
            redisTemplate.delete("post:feed");

            log.info("<<< deletePost EXIT | postId={} | userId={} | traceId={}", postId, userId, LogUtil.getTraceId());
        } catch (Exception e) {
            log.error("!!! deletePost ERROR | postId={} | userId={} | error={}", postId, userId, e.getMessage());
            throw e;
        }
    }

    @Override
    public PageResult<PostDTO> getUserPosts(Long userId, Integer page, Integer size) {
        log.debug(">>> getUserPosts ENTER | userId={} | page={} | size={}", userId, page, size);
        try {
            PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Post> postPage = postRepository.findByUserId(userId, pageRequest);

            List<PostDTO> dtoList = postPage.getContent().stream()
                    .map(this::toPostDTO)
                    .collect(Collectors.toList());

            PageResult<PostDTO> result = PageResult.of(dtoList, postPage.getTotalElements(), page, size);
            log.debug("<<< getUserPosts EXIT | userId={} | total={} | traceId={}", userId, result.getTotal(), LogUtil.getTraceId());
            return result;
        } catch (Exception e) {
            log.error("!!! getUserPosts ERROR | userId={} | error={}", userId, e.getMessage());
            throw e;
        }
    }

    @Override
    public PageResult<PostDTO> getFeed(Integer page, Integer size) {
        log.debug(">>> getFeed ENTER | page={} | size={}", page, size);
        try {
            PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Post> postPage = postRepository.findAll(pageRequest);

            List<PostDTO> dtoList = postPage.getContent().stream()
                    .map(this::toPostDTO)
                    .collect(Collectors.toList());

            PageResult<PostDTO> result = PageResult.of(dtoList, postPage.getTotalElements(), page, size);
            log.debug("<<< getFeed EXIT | total={} | traceId={}", result.getTotal(), LogUtil.getTraceId());
            return result;
        } catch (Exception e) {
            log.error("!!! getFeed ERROR | error={}", e.getMessage());
            throw e;
        }
    }

    @Override
    public PageResult<PostDTO> searchPosts(String keyword, Integer page, Integer size) {
        log.debug(">>> searchPosts ENTER | keyword={} | page={} | size={}", keyword, page, size);
        try {
            PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Post> postPage;

            if (StringUtils.hasText(keyword)) {
                postPage = postRepository.searchByKeyword(keyword, pageRequest);
            } else {
                postPage = postRepository.findAll(pageRequest);
            }

            List<PostDTO> dtoList = postPage.getContent().stream()
                    .map(this::toPostDTO)
                    .collect(Collectors.toList());

            PageResult<PostDTO> result = PageResult.of(dtoList, postPage.getTotalElements(), page, size);
            log.debug("<<< searchPosts EXIT | keyword={} | total={} | traceId={}", keyword, result.getTotal(), LogUtil.getTraceId());
            return result;
        } catch (Exception e) {
            log.error("!!! searchPosts ERROR | keyword={} | error={}", keyword, e.getMessage());
            throw e;
        }
    }

    private PostDTO toPostDTO(Post post) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setUserId(post.getUserId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setType(post.getType());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setVideoUrl(post.getVideoUrl());
        dto.setLikeCount(post.getLikeCount() != null ? post.getLikeCount() : 0);
        dto.setCommentCount(post.getCommentCount() != null ? post.getCommentCount() : 0);

        // Parse imageUrls JSON string to List
        if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
            try {
                List<String> imageUrls = objectMapper.readValue(post.getImageUrls(), new TypeReference<List<String>>() {});
                dto.setImageUrls(imageUrls);
            } catch (JsonProcessingException e) {
                dto.setImageUrls(Collections.emptyList());
            }
        }

        return dto;
    }
}
