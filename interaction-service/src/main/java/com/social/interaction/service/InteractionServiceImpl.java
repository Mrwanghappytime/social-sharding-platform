package com.social.interaction.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.social.common.api.NotificationService;
import com.social.common.api.PostService;
import com.social.common.api.UserService;
import com.social.common.api.InteractionService;
import com.social.common.dto.UserDTO;
import com.social.common.enums.NotificationType;
import com.social.common.exception.BusinessException;
import com.social.common.exception.ErrorCode;
import com.social.common.repository.CommentRepository;
import com.social.common.repository.LikeRepository;
import com.social.common.dto.CommentDTO;
import com.social.common.dto.LikeStatusDTO;
import com.social.common.entity.Comment;
import com.social.common.entity.Like;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@DubboService(version = "1.0.0")
public class InteractionServiceImpl implements InteractionService {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private CommentRepository commentRepository;

    @DubboReference(version = "1.0.0", check = false)
    private PostService postService;

    @DubboReference(version = "1.0.0", check = false)
    private UserService userService;

    @DubboReference(version = "1.0.0", check = false)
    private NotificationService notificationService;

    @Override
    @Transactional
    public void likePost(Long postId, Long userId) {
        if (!postService.isPostExists(postId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND, "动态不存在");
        }

        Like like = new Like();
        like.setPostId(postId);
        like.setUserId(userId);

        try {
            likeRepository.save(like);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED, "已经点赞过该动态");
        }

        postService.incrementLikeCount(postId);

        Long postOwnerId = postService.getUserIdByPostId(postId);
        if (!postOwnerId.equals(userId)) {
            notificationService.sendNotification(postOwnerId, NotificationType.LIKE, userId, postId, "POST");
        }
    }

    @Override
    @Transactional
    public void unlikePost(Long postId, Long userId) {
        Optional<Like> likeOpt = likeRepository.findByUserIdAndPostId(userId, postId);
        if (likeOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_LIKED, "未点赞该动态");
        }
        likeRepository.delete(likeOpt.get());
        postService.decrementLikeCount(postId);
    }

    @Override
    public LikeStatusDTO getLikeStatus(Long postId, Long userId) {
        if (!postService.isPostExists(postId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND, "动态不存在");
        }

        long likeCount = likeRepository.countByPostId(postId);
        boolean userLiked = likeRepository.existsByUserIdAndPostId(userId, postId).isPresent();

        LikeStatusDTO dto = new LikeStatusDTO();
        dto.setLiked(userLiked);
        dto.setLikeCount(likeCount);

        return dto;
    }

    @Override
    @Transactional
    public CommentDTO commentOnPost(Long postId, Long userId, String content) {
        if (!postService.isPostExists(postId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND, "动态不存在");
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);

        Comment savedComment = commentRepository.save(comment);

        postService.incrementCommentCount(postId);

        Long postOwnerId = postService.getUserIdByPostId(postId);
        if (!postOwnerId.equals(userId)) {
            notificationService.sendNotification(postOwnerId, NotificationType.COMMENT, userId, postId, "POST");
        }

        UserDTO user = userService.getUserById(userId);
        return toCommentDTO(savedComment, user);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "评论不存在"));

        Long postOwnerId = postService.getUserIdByPostId(comment.getPostId());
        if (!comment.getUserId().equals(userId) && !postOwnerId.equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除该评论");
        }

        Long postId = comment.getPostId();
        commentRepository.deleteById(commentId);
        postService.decrementCommentCount(postId);
    }

    @Override
    public List<CommentDTO> getComments(Long postId, Integer page, Integer size) {
        if (!postService.isPostExists(postId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND, "动态不存在");
        }

        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Comment> commentPage = commentRepository.findByPostId(postId, pageRequest);

        List<Long> userIds = commentPage.getContent().stream()
                .map(Comment::getUserId)
                .distinct()
                .toList();

        Map<Long, UserDTO> userMap = userIds.stream()
                .collect(Collectors.toMap(
                        uid -> uid,
                        uid -> {
                            try {
                                return userService.getUserById(uid);
                            } catch (Exception e) {
                                return null;
                            }
                        }
                ));

        return commentPage.getContent().stream()
                .map(comment -> toCommentDTO(comment, userMap.get(comment.getUserId())))
                .collect(Collectors.toList());
    }

    private CommentDTO toCommentDTO(Comment comment, UserDTO user) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setPostId(comment.getPostId());
        dto.setUserId(comment.getUserId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());

        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setUserAvatar(user.getAvatar());
        }

        return dto;
    }
}
