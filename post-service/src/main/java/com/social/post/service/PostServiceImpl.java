package com.social.post.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.common.api.InteractionService;
import com.social.common.api.PostService;
import com.social.common.dto.PostDTO;
import com.social.common.dto.PageResult;
import com.social.common.entity.Post;
import com.social.common.exception.BusinessException;
import com.social.common.exception.ErrorCode;
import com.social.common.repository.PostRepository;
import org.apache.dubbo.config.annotation.DubboReference;
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

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @DubboReference(version = "1.0.0", check = false)
    private InteractionService interactionService;

    private static final String POST_CACHE_KEY = "post:";
    private static final String USER_POSTS_KEY = "user:posts:";

    @Override
    public PostDTO getPostById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND, "动态不存在"));
        return toPostDTO(post);
    }

    @Override
    public boolean isPostExists(Long postId) {
        return postRepository.existsById(postId);
    }

    @Override
    public Long getUserIdByPostId(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND, "动态不存在"));
        return post.getUserId();
    }

    @Override
    @Transactional
    public void incrementLikeCount(Long postId) {
        postRepository.findById(postId).ifPresent(post -> {
            post.setLikeCount(post.getLikeCount() == null ? 1 : post.getLikeCount() + 1);
            postRepository.save(post);
        });
    }

    @Override
    @Transactional
    public void decrementLikeCount(Long postId) {
        postRepository.findById(postId).ifPresent(post -> {
            post.setLikeCount(post.getLikeCount() == null || post.getLikeCount() <= 0 ? 0 : post.getLikeCount() - 1);
            postRepository.save(post);
        });
    }

    @Override
    @Transactional
    public void incrementCommentCount(Long postId) {
        postRepository.findById(postId).ifPresent(post -> {
            post.setCommentCount(post.getCommentCount() == null ? 1 : post.getCommentCount() + 1);
            postRepository.save(post);
        });
    }

    @Override
    @Transactional
    public void decrementCommentCount(Long postId) {
        postRepository.findById(postId).ifPresent(post -> {
            post.setCommentCount(post.getCommentCount() == null || post.getCommentCount() <= 0 ? 0 : post.getCommentCount() - 1);
            postRepository.save(post);
        });
    }

    @Override
    public boolean hasUserLikedPost(Long postId, Long userId) {
        try {
            return interactionService.getLikeStatus(postId, userId).getLiked();
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public Post createPost(Long userId, String title, String content, com.social.common.enums.PostType type, String imageUrls, String videoUrl) {
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

        return savedPost;
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND, "动态不存在"));

        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除此动态");
        }

        postRepository.deleteById(postId);

        redisTemplate.delete(POST_CACHE_KEY + postId);
        redisTemplate.delete(USER_POSTS_KEY + userId);
        redisTemplate.delete("post:feed");
    }

    public PageResult<PostDTO> getUserPosts(Long userId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> postPage = postRepository.findByUserId(userId, pageRequest);

        List<PostDTO> dtoList = postPage.getContent().stream()
                .map(this::toPostDTO)
                .collect(Collectors.toList());

        return PageResult.of(dtoList, postPage.getTotalElements(), page, size);
    }

    public PageResult<PostDTO> getFeed(Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> postPage = postRepository.findAll(pageRequest);

        List<PostDTO> dtoList = postPage.getContent().stream()
                .map(this::toPostDTO)
                .collect(Collectors.toList());

        return PageResult.of(dtoList, postPage.getTotalElements(), page, size);
    }

    public PageResult<PostDTO> searchPosts(String keyword, Integer page, Integer size) {
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

        return PageResult.of(dtoList, postPage.getTotalElements(), page, size);
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
